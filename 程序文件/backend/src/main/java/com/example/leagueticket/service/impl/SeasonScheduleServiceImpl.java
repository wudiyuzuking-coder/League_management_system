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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class SeasonScheduleServiceImpl implements SeasonScheduleService {
    private static final int MIN_ROUND_INTERVAL_DAYS=6;
    private static final Set<String> BATCH_STATUSES=Set.of("GENERATED","CONFIRMED");
    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final RoundInfoMapper roundMapper;
    private final MatchInfoMapper matchMapper;
    private final ClubSeasonRecordMapper recordMapper;
    private final SystemConfigMapper configMapper;
    private final SystemTimeService timeService;

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
        if(scheduleMapper.countSeasonMatches(seasonId)>0)throw conflict("赛季已存在人工比赛，无法直接执行自动排赛");

        int rounds=roundCount(teams.size());
        int matches=teams.size()*(teams.size()-1);
        long totalDays=ChronoUnit.DAYS.between(season.getStartDate(),season.getEndDate());
        long minimum=(long)(rounds-1)*MIN_ROUND_INTERVAL_DAYS;
        if(totalDays<minimum)throw conflict("赛季日期范围不足以满足双循环赛程及6天比赛间隔");
        long interval=rounds==1?0:totalDays/(rounds-1);
        LocalTime kickoff=kickoffTime();
        List<List<Pair>> fixtures=fixtures(teams);
        Map<Integer,RoundInfo> existingRounds=new HashMap<>();
        for(RoundInfo r:roundMapper.findBySeasonId(seasonId))existingRounds.put(r.getRoundNo(),r);

        SeasonScheduleBatch batch=new SeasonScheduleBatch();
        batch.setSeasonId(seasonId);batch.setTriggerType(triggerType);batch.setClubCount(teams.size());
        batch.setRoundCount(rounds);batch.setMatchCount(matches);batch.setGeneratedAt(now);scheduleMapper.insertBatch(batch);
        for(int i=0;i<fixtures.size();i++){
            int roundNo=i+1;LocalDate date=season.getStartDate().plusDays(interval*i);
            RoundInfo round=existingRounds.get(roundNo);
            if(round==null){round=new RoundInfo();round.setSeasonId(seasonId);round.setRoundNo(roundNo);round.setRoundName("第"+roundNo+"轮");round.setStartDate(date);round.setEndDate(date);round.setRoundStatus("DRAFT");roundMapper.insert(round);}
            else validateReusableRound(round,date);
            for(Pair pair:fixtures.get(i)){
                SeasonScheduleMapper.EnrollmentTeam home=teams.get(pair.homeIndex());
                SeasonScheduleMapper.EnrollmentTeam away=teams.get(pair.awayIndex());
                MatchInfo match=new MatchInfo();match.setSeasonId(seasonId);match.setRoundId(round.getRoundId());
                match.setHomeClubId(home.getClubId());match.setAwayClubId(away.getClubId());match.setStadiumId(home.getStadiumId());
                match.setMatchTime(LocalDateTime.of(date,kickoff));matchMapper.insert(match);scheduleMapper.insertMatchLink(batch.getBatchId(),match.getMatchId());
            }
        }
        return detail(scheduleMapper.findBySeason(seasonId));
    }

    @Override public ScheduleDetailResponse get(Long seasonId){SeasonScheduleBatch b=scheduleMapper.findBySeason(seasonId);if(b==null)throw new BusinessException(HttpStatus.NOT_FOUND,"schedule not found");return detail(b);}
    @Override public PageResponse<SeasonScheduleBatch> list(ScheduleQueryRequest q){if(q.batchStatus()!=null&&!q.batchStatus().isBlank()&&!BATCH_STATUSES.contains(q.batchStatus()))throw new BusinessException("invalid batch status");int page=q.safePage(),size=q.safeSize();long total=scheduleMapper.countPage(q);return new PageResponse<>(scheduleMapper.findPage(q,(long)(page-1)*size,size),total,page,size);}

    @Override @Transactional
    public ScheduleDetailResponse confirm(Long seasonId,Long userId){
        SeasonInfo season=seasonMapper.findByIdForUpdate(seasonId);if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        SeasonScheduleBatch batch=scheduleMapper.findBySeason(seasonId);if(batch==null)throw new BusinessException(HttpStatus.NOT_FOUND,"schedule not found");
        if("GENERATED".equals(batch.getBatchStatus()))scheduleMapper.confirm(seasonId,userId,timeService.now());
        for(SeasonScheduleMapper.EnrollmentTeam team:scheduleMapper.findTeams(seasonId))recordMapper.ensureRecord(seasonId,team.getClubId());
        return detail(scheduleMapper.findBySeason(seasonId));
    }

    @Override public List<ClubScheduleResponse> clubSchedules(Long clubId){LocalDate now=timeService.now().toLocalDate();List<ClubScheduleResponse> rows=scheduleMapper.findConfirmedForClub(clubId);rows.forEach(r->r.setDaysUntilMatch(ChronoUnit.DAYS.between(now,r.getMatchDateTime().toLocalDate())));return rows;}

    private ScheduleDetailResponse detail(SeasonScheduleBatch batch){ScheduleDetailResponse out=new ScheduleDetailResponse();BeanUtils.copyProperties(batch,out);Map<Integer,List<ScheduleMatchResponse>> grouped=new LinkedHashMap<>();for(ScheduleMatchResponse m:scheduleMapper.findBatchMatches(batch.getBatchId()))grouped.computeIfAbsent(m.getRoundNo(),k->new ArrayList<>()).add(m);out.setRounds(grouped.entrySet().stream().map(e->new ScheduleRoundResponse(e.getKey(),e.getValue())).toList());return out;}
    private LocalTime kickoffTime(){String value=configMapper.findEnabledValue("AUTO_SCHEDULE_DEFAULT_KICKOFF_TIME");try{return LocalTime.parse(value==null?"19:30":value);}catch(DateTimeParseException e){throw new BusinessException("AUTO_SCHEDULE_DEFAULT_KICKOFF_TIME配置格式必须为HH:mm");}}
    private void validateReusableRound(RoundInfo round,LocalDate date){if(!"DRAFT".equals(round.getRoundStatus())||round.getStartDate()==null||round.getEndDate()==null||date.isBefore(round.getStartDate())||date.isAfter(round.getEndDate()))throw conflict("现有轮次无法安全复用于自动排赛: 第"+round.getRoundNo()+"轮");}
    private int roundCount(int clubs){return clubs%2==0?2*(clubs-1):2*clubs;}
    private List<List<Pair>> fixtures(List<SeasonScheduleMapper.EnrollmentTeam> teams){int n=teams.size();int size=n%2==0?n:n+1;List<Integer> rotating=new ArrayList<>();for(int i=0;i<n;i++)rotating.add(i);if(size>n)rotating.add(null);List<List<Pair>> first=new ArrayList<>();for(int round=0;round<size-1;round++){List<Pair> games=new ArrayList<>();for(int i=0;i<size/2;i++){Integer left=rotating.get(i),right=rotating.get(size-1-i);if(left==null||right==null)continue;if((round+i)%2==0)games.add(new Pair(left,right));else games.add(new Pair(right,left));}first.add(games);Integer last=rotating.remove(size-1);rotating.add(1,last);}List<List<Pair>> all=new ArrayList<>(first);for(List<Pair> round:first)all.add(round.stream().map(p->new Pair(p.awayIndex(),p.homeIndex())).toList());return all;}
    private BusinessException conflict(String message){return new BusinessException(HttpStatus.CONFLICT,message);}
    private record Pair(int homeIndex,int awayIndex){}
}
