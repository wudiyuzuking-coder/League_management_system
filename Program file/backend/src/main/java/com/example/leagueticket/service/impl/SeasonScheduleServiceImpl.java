package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class SeasonScheduleServiceImpl implements SeasonScheduleService {
    private static final Set<String> BATCH_STATUSES=Set.of("GENERATED","CONFIRMED");
    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final ClubSeasonEnrollmentMapper enrollmentMapper;
    private final RoundInfoMapper roundMapper;
    private final MatchInfoMapper matchMapper;
    private final ClubSeasonRecordMapper recordMapper;
    private final SystemTimeService timeService;
    private final DoubleRoundRobinSchedulePlanner planner;
    private final TicketSalePolicy ticketSalePolicy;
    private final PublicSeasonVisibilityService publicVisibility;

    @Override @Transactional
    public ScheduleDetailResponse generateIfEligible(Long seasonId,String triggerType){
        SeasonInfo season=seasonMapper.findByIdForUpdate(seasonId);
        if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        SeasonScheduleBatch existing=scheduleMapper.findBySeason(seasonId);
        if(existing!=null)return detail(existing);
        if(!"DRAFT".equals(season.getSeasonStatus()))throw conflict("仅DRAFT赛季可以自动排赛");
        if(season.getRegistrationDeadline()==null||season.getMaxClubs()==null)throw conflict("赛季报名配置不完整");
        List<SeasonScheduleMapper.EnrollmentTeam> teams=scheduleMapper.findTeams(seasonId);
        LocalDateTime now=timeService.now();
        boolean full=teams.size()>=season.getMaxClubs();
        boolean deadline=!now.isBefore(season.getRegistrationDeadline());
        if(!full&&!deadline)throw conflict("赛季未满额且报名尚未截止，暂不能生成赛程");
        if(teams.size()<2)throw conflict("参赛俱乐部不足，至少需要2支球队");
        return generateLocked(season,teams,triggerType,now);
    }

    @Override @Transactional
    public ScheduleDetailResponse closeRegistrationAndGenerate(Long seasonId){
        SeasonInfo season=seasonMapper.findByIdForUpdate(seasonId);
        if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        if(!"DRAFT".equals(season.getSeasonStatus()))throw conflict("仅DRAFT赛季可以提前截止报名");
        if(scheduleMapper.findBySeason(seasonId)!=null)throw conflict("报名已关闭或赛程已生成");
        enrollmentMapper.findSubmittedIdsForUpdate(seasonId);
        List<SeasonScheduleMapper.EnrollmentTeam> teams=scheduleMapper.findTeams(seasonId);
        if(teams.size()<2)throw conflict("当前报名球队不足，无法生成赛程");
        return generateLocked(season,teams,"MANUAL",timeService.now());
    }

    private ScheduleDetailResponse generateLocked(SeasonInfo season,List<SeasonScheduleMapper.EnrollmentTeam> teams,String triggerType,LocalDateTime now){
        Long seasonId=season.getSeasonId();
        if(scheduleMapper.countSeasonMatches(seasonId)>0)throw conflict("赛季已存在人工比赛，无法直接执行自动排赛");

        List<DoubleRoundRobinSchedulePlanner.PlannedGame> games=planner.plan(teams.size(),season.getStartDate());
        int rounds=games.stream().mapToInt(DoubleRoundRobinSchedulePlanner.PlannedGame::roundNo).max().orElse(0);
        int matches=games.size();LocalTime kickoff=LocalTime.of(20,0);
        Map<Integer,RoundInfo> existingRounds=new HashMap<>();
        for(RoundInfo r:roundMapper.findBySeasonId(seasonId))existingRounds.put(r.getRoundNo(),r);

        SeasonScheduleBatch batch=new SeasonScheduleBatch();
        batch.setSeasonId(seasonId);batch.setTriggerType(triggerType);batch.setClubCount(teams.size());
        batch.setRoundCount(rounds);batch.setMatchCount(matches);batch.setGeneratedAt(now);scheduleMapper.insertBatch(batch);
        for(int roundNo=1;roundNo<=rounds;roundNo++){
            int currentRound=roundNo;List<DoubleRoundRobinSchedulePlanner.PlannedGame> roundGames=games.stream().filter(g->g.roundNo()==currentRound).toList();
            LocalDate start=roundGames.get(0).date(),end=roundGames.get(roundGames.size()-1).date();
            RoundInfo round=existingRounds.get(roundNo);
            if(round==null){round=new RoundInfo();round.setSeasonId(seasonId);round.setRoundNo(roundNo);round.setRoundName("第"+roundNo+"轮");round.setStartDate(start);round.setEndDate(end);round.setRoundStatus("DRAFT");roundMapper.insert(round);}
            else validateReusableRound(round,start,end);
            for(DoubleRoundRobinSchedulePlanner.PlannedGame game:roundGames){
                SeasonScheduleMapper.EnrollmentTeam home=teams.get(game.homeIndex());
                SeasonScheduleMapper.EnrollmentTeam away=teams.get(game.awayIndex());
                MatchInfo match=new MatchInfo();match.setSeasonId(seasonId);match.setRoundId(round.getRoundId());
                match.setHomeClubId(home.getClubId());match.setAwayClubId(away.getClubId());match.setStadiumId(home.getStadiumId());
                match.setMatchTime(LocalDateTime.of(game.date(),kickoff));ticketSalePolicy.applySaleWindow(match);matchMapper.insert(match);scheduleMapper.insertMatchLink(batch.getBatchId(),match.getMatchId());
            }
        }
        return detail(scheduleMapper.findBySeason(seasonId));
    }

    @Override public ScheduleDetailResponse get(Long seasonId){SeasonScheduleBatch b=scheduleMapper.findBySeason(seasonId);if(b==null)throw new BusinessException(HttpStatus.NOT_FOUND,"schedule not found");return detail(b);}
    @Override public UserSeasonScheduleResponse getPublicConfirmed(Long seasonId){
        publicVisibility.requirePublicVisibleSeason(seasonId);
        SeasonInfo season=seasonMapper.findById(seasonId);if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        SeasonScheduleBatch batch=scheduleMapper.findBySeason(seasonId);if(batch==null||!"CONFIRMED".equals(batch.getBatchStatus()))throw new BusinessException(HttpStatus.NOT_FOUND,"confirmed schedule not found");
        Map<Integer,List<ScheduleMatchResponse>> grouped=new LinkedHashMap<>();
        for(ScheduleMatchResponse row:scheduleMapper.findConfirmedPublicMatches(seasonId)){
            MatchInfo match=new MatchInfo();match.setMatchStatus(row.getMatchStatus());match.setMatchTime(row.getMatchDateTime());
            ticketSalePolicy.applySaleWindow(match);row.setSaleStartTime(match.getSaleStartTime());row.setSaleEndTime(match.getSaleEndTime());
            TicketSalePolicy.SaleEvaluation sale=ticketSalePolicy.evaluateMatchAvailability(match,timeService.now(),row.getRemainingTickets(),row.getOnSaleZoneCount()>0);
            row.setPurchasable(sale.available());row.setSaleStatus("AVAILABLE".equals(sale.state())?"ON_SALE":sale.state());
            grouped.computeIfAbsent(row.getRoundNo(),key->new ArrayList<>()).add(row);
        }
        return new UserSeasonScheduleResponse(seasonId,season.getSeasonName(),season.getStartDate(),season.getEndDate(),batch.getClubCount(),grouped.entrySet().stream().map(e->new ScheduleRoundResponse(e.getKey(),e.getValue())).toList());
    }
    @Override public PageResponse<SeasonScheduleBatch> list(ScheduleQueryRequest q){if(q.batchStatus()!=null&&!q.batchStatus().isBlank()&&!BATCH_STATUSES.contains(q.batchStatus()))throw new BusinessException("invalid batch status");int page=q.safePage(),size=q.safeSize();long total=scheduleMapper.countPage(q);return new PageResponse<>(scheduleMapper.findPage(q,(long)(page-1)*size,size),total,page,size);}

    @Override @Transactional
    public ScheduleDetailResponse confirm(Long seasonId,Long userId){
        SeasonInfo season=seasonMapper.findByIdForUpdate(seasonId);if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        SeasonScheduleBatch batch=scheduleMapper.findBySeason(seasonId);if(batch==null)throw new BusinessException(HttpStatus.NOT_FOUND,"schedule not found");
        LocalDateTime now=timeService.now();if("GENERATED".equals(batch.getBatchStatus()))scheduleMapper.confirm(seasonId,userId,now);
        scheduleMapper.publishConfirmedRounds(seasonId);scheduleMapper.publishConfirmedMatches(seasonId,now);
        for(SeasonScheduleMapper.EnrollmentTeam team:scheduleMapper.findTeams(seasonId))recordMapper.ensureRecord(seasonId,team.getClubId());
        return detail(scheduleMapper.findBySeason(seasonId));
    }

    @Override public List<ClubScheduleResponse> clubSchedules(Long clubId){LocalDate now=timeService.now().toLocalDate();List<ClubScheduleResponse> rows=scheduleMapper.findConfirmedForClub(clubId);rows.forEach(r->r.setDaysUntilMatch(ChronoUnit.DAYS.between(now,r.getMatchDateTime().toLocalDate())));return rows;}

    private ScheduleDetailResponse detail(SeasonScheduleBatch batch){ScheduleDetailResponse out=new ScheduleDetailResponse();BeanUtils.copyProperties(batch,out);Map<Integer,List<ScheduleMatchResponse>> grouped=new LinkedHashMap<>();for(ScheduleMatchResponse m:scheduleMapper.findBatchMatches(batch.getBatchId()))grouped.computeIfAbsent(m.getRoundNo(),k->new ArrayList<>()).add(m);out.setRounds(grouped.entrySet().stream().map(e->new ScheduleRoundResponse(e.getKey(),e.getValue())).toList());return out;}
    private void validateReusableRound(RoundInfo round,LocalDate start,LocalDate end){if(!"DRAFT".equals(round.getRoundStatus())||round.getStartDate()==null||round.getEndDate()==null||start.isBefore(round.getStartDate())||end.isAfter(round.getEndDate()))throw conflict("现有轮次无法安全复用于自动排赛: 第"+round.getRoundNo()+"轮");}
    private BusinessException conflict(String message){return new BusinessException(HttpStatus.CONFLICT,message);}
}
