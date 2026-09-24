package com.example.leagueticket.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.RoundInfo;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubSeasonRecordMapper;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.RoundInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.mapper.SeasonScheduleMapper;
import com.example.leagueticket.service.DoubleRoundRobinSchedulePlanner;
import com.example.leagueticket.service.PublicSeasonVisibilityService;
import com.example.leagueticket.service.SeasonLifecycleService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.service.TicketSalePolicy;
import com.example.leagueticket.service.MatchTicketZoneService;
import com.example.leagueticket.vo.ClubScheduleResponse;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.ScheduleDetailResponse;
import com.example.leagueticket.vo.ScheduleMatchResponse;
import com.example.leagueticket.vo.ScheduleRoundResponse;
import com.example.leagueticket.vo.UserSeasonScheduleResponse;

import lombok.RequiredArgsConstructor;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SeasonScheduleServiceImpl implements SeasonScheduleService {
    private static final Set<String> BATCH_STATUSES = Set.of("GENERATED", "CONFIRMED");
    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final RoundInfoMapper roundMapper;
    private final MatchInfoMapper matchMapper;
    private final ClubSeasonRecordMapper recordMapper;
    private final SystemTimeService timeService;
    private final DoubleRoundRobinSchedulePlanner planner;
    private final TicketSalePolicy ticketSalePolicy;
    private final PublicSeasonVisibilityService publicVisibility;
    private final SeasonLifecycleService lifecycleService;
    private final MatchTicketZoneService ticketZoneService;

    @Override
    @Transactional
    public ScheduleDetailResponse generateIfEligible(Long seasonId, String triggerType) {
        SeasonInfo season = seasonMapper.findByIdForUpdate(seasonId);
        if (season == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        SeasonScheduleBatch existing = scheduleMapper.findBySeason(seasonId);
        if (existing != null) return completeExistingOrReturn(season,existing,null);
        if (!SeasonStatus.REGISTRATION.matches(season.getSeasonStatus()))
            throw conflict("仅报名中的赛季可以自动排赛");
        if (season.getRegistrationDeadline() == null || season.getMaxClubs() == null)
            throw conflict("赛季报名配置不完整");
        List<SeasonScheduleMapper.EnrollmentTeam> teams = scheduleMapper.findTeams(seasonId);
        LocalDateTime now = timeService.now();
        boolean full = teams.size() >= season.getMaxClubs();
        boolean deadline = !now.isBefore(season.getRegistrationDeadline());
        if (!full && !deadline)
            throw conflict("赛季未满额且报名尚未截止，暂不能生成赛程");
        if (teams.size() < 2)
            throw conflict("参赛俱乐部不足，至少需要2支球队");
        return closeGenerateAndPublishLocked(season,teams,triggerType,null,now);
    }

    @Override
    @Transactional
    public ScheduleDetailResponse closeRegistrationAndPublishSchedule(Long seasonId,Long confirmedBy) {
        SeasonInfo season = seasonMapper.findByIdForUpdate(seasonId);
        if (season == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        SeasonScheduleBatch existing=scheduleMapper.findBySeason(seasonId);
        if(existing!=null)return completeExistingOrReturn(season,existing,confirmedBy);
        if (!SeasonStatus.REGISTRATION.matches(season.getSeasonStatus()))
            throw conflict("仅报名中的赛季可以提前截止报名");
        List<SeasonScheduleMapper.EnrollmentTeam> teams = scheduleMapper.findTeams(seasonId);
        if (teams.size() < 2)
            throw conflict("当前报名球队不足，无法生成赛程");
        return closeGenerateAndPublishLocked(season,teams,"MANUAL",confirmedBy,timeService.now());
    }

    private ScheduleDetailResponse closeGenerateAndPublishLocked(SeasonInfo season,List<SeasonScheduleMapper.EnrollmentTeam> teams,
            String triggerType,Long confirmedBy,LocalDateTime now){
        lifecycleService.closeRegistration(season.getSeasonId());
        SeasonScheduleBatch batch=generateLocked(season,teams,triggerType,now);
        return publishAndConfirmLocked(season.getSeasonId(),batch,teams,confirmedBy,now);
    }

    private SeasonScheduleBatch generateLocked(SeasonInfo season, List<SeasonScheduleMapper.EnrollmentTeam> teams,
            String triggerType, LocalDateTime now) {
        Long seasonId = season.getSeasonId();
        if (scheduleMapper.countSeasonMatches(seasonId) > 0)
            throw conflict("赛季已存在人工比赛，无法直接执行自动排赛");

        List<DoubleRoundRobinSchedulePlanner.PlannedGame> games = planner.plan(teams.size(), season.getStartDate());
        int rounds = games.stream().mapToInt(DoubleRoundRobinSchedulePlanner.PlannedGame::roundNo).max().orElse(0);
        int matches = games.size();
        LocalTime kickoff = LocalTime.of(20, 0);
        Map<Integer, RoundInfo> existingRounds = new HashMap<>();
        for (RoundInfo r : roundMapper.findBySeasonId(seasonId))
            existingRounds.put(r.getRoundNo(), r);

        SeasonScheduleBatch batch = new SeasonScheduleBatch();
        batch.setSeasonId(seasonId);
        batch.setTriggerType(triggerType);
        batch.setClubCount(teams.size());
        batch.setRoundCount(rounds);
        batch.setMatchCount(matches);
        batch.setGeneratedAt(now);
        scheduleMapper.insertBatch(batch);
        for (int roundNo = 1; roundNo <= rounds; roundNo++) {
            int currentRound = roundNo;
            List<DoubleRoundRobinSchedulePlanner.PlannedGame> roundGames = games.stream()
                    .filter(g -> g.roundNo() == currentRound).toList();
            LocalDate start = roundGames.get(0).date(), end = roundGames.get(roundGames.size() - 1).date();
            RoundInfo round = existingRounds.get(roundNo);
            if (round == null) {
                round = new RoundInfo();
                round.setSeasonId(seasonId);
                round.setRoundNo(roundNo);
                round.setRoundName("第" + roundNo + "轮");
                round.setStartDate(start);
                round.setEndDate(end);
                round.setRoundStatus("DRAFT");
                roundMapper.insert(round);
            } else
                validateReusableRound(round, start, end);
            for (DoubleRoundRobinSchedulePlanner.PlannedGame game : roundGames) {
                SeasonScheduleMapper.EnrollmentTeam home = teams.get(game.homeIndex());
                SeasonScheduleMapper.EnrollmentTeam away = teams.get(game.awayIndex());
                MatchInfo match = new MatchInfo();
                match.setSeasonId(seasonId);
                match.setRoundId(round.getRoundId());
                match.setHomeClubId(home.getClubId());
                match.setAwayClubId(away.getClubId());
                match.setStadiumId(home.getStadiumId());
                match.setMatchTime(LocalDateTime.of(game.date(), kickoff));
                ticketSalePolicy.applySaleWindow(match);
                matchMapper.insert(match);
                scheduleMapper.insertMatchLink(batch.getBatchId(), match.getMatchId());
            }
        }
        return scheduleMapper.findBySeason(seasonId);
    }

    private ScheduleDetailResponse completeExistingOrReturn(SeasonInfo season,SeasonScheduleBatch batch,Long confirmedBy){
        if("CONFIRMED".equals(batch.getBatchStatus()))return detail(batch);
        if(!SeasonStatus.PREPARING.matches(season.getSeasonStatus()))throw conflict("报名已关闭或赛程已生成");
        List<SeasonScheduleMapper.EnrollmentTeam> teams=scheduleMapper.findTeams(season.getSeasonId());
        if(teams.size()<2)throw conflict("参赛俱乐部不足，至少需要2支球队");
        return publishAndConfirmLocked(season.getSeasonId(),batch,teams,confirmedBy,timeService.now());
    }

    private ScheduleDetailResponse publishAndConfirmLocked(Long seasonId,SeasonScheduleBatch batch,
            List<SeasonScheduleMapper.EnrollmentTeam> teams,Long confirmedBy,LocalDateTime now){
        scheduleMapper.publishConfirmedRounds(seasonId);
        scheduleMapper.publishConfirmedMatches(seasonId,now);
        if(scheduleMapper.countUnpublishedScheduledMatches(seasonId)>0)
            throw conflict("赛程中的比赛未能全部发布");
        for(SeasonScheduleMapper.EnrollmentTeam team:teams)recordMapper.ensureRecord(seasonId,team.getClubId());
        for(ScheduleMatchResponse match:scheduleMapper.findBatchMatches(batch.getBatchId()))
            ticketZoneService.initializeStandardAutomatically(match.getMatchId());
        if(scheduleMapper.confirm(seasonId,confirmedBy,now)!=1)
            throw conflict("赛程确认状态已变化，请刷新后重试");
        return detail(scheduleMapper.findBySeason(seasonId));
    }

    @Override
    public ScheduleDetailResponse get(Long seasonId) {
        SeasonScheduleBatch b = scheduleMapper.findBySeason(seasonId);
        if (b == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "schedule not found");
        return detail(b);
    }

    @Override
    public UserSeasonScheduleResponse getPublicConfirmed(Long seasonId) {
        publicVisibility.requirePublicVisibleSeason(seasonId);
        SeasonInfo season = seasonMapper.findById(seasonId);
        if (season == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        SeasonScheduleBatch batch = scheduleMapper.findBySeason(seasonId);
        if (batch == null || !"CONFIRMED".equals(batch.getBatchStatus()))
            throw new BusinessException(HttpStatus.NOT_FOUND, "confirmed schedule not found");
        Map<Integer, List<ScheduleMatchResponse>> grouped = new LinkedHashMap<>();
        for (ScheduleMatchResponse row : scheduleMapper.findConfirmedPublicMatches(seasonId)) {
            MatchInfo match = new MatchInfo();
            match.setMatchStatus(row.getMatchStatus());
            match.setMatchTime(row.getMatchDateTime());
            ticketSalePolicy.applySaleWindow(match);
            row.setSaleStartTime(match.getSaleStartTime());
            row.setSaleEndTime(match.getSaleEndTime());
            TicketSalePolicy.SaleEvaluation sale = ticketSalePolicy.evaluateMatchAvailability(match, timeService.now(),
                    row.getRemainingTickets(), row.getOnSaleZoneCount() > 0);
            row.setPurchasable(sale.available());
            row.setSaleStatus("AVAILABLE".equals(sale.state()) ? "ON_SALE" : sale.state());
            grouped.computeIfAbsent(row.getRoundNo(), key -> new ArrayList<>()).add(row);
        }
        return new UserSeasonScheduleResponse(seasonId, season.getSeasonName(), season.getStartDate(),
                season.getEndDate(), batch.getClubCount(),
                grouped.entrySet().stream().map(e -> new ScheduleRoundResponse(e.getKey(), e.getValue())).toList());
    }

    @Override
    public PageResponse<SeasonScheduleBatch> list(ScheduleQueryRequest q) {
        if (q.batchStatus() != null && !q.batchStatus().isBlank() && !BATCH_STATUSES.contains(q.batchStatus()))
            throw new BusinessException("invalid batch status");
        int page = q.safePage(), size = q.safeSize();
        long total = scheduleMapper.countPage(q);
        return new PageResponse<>(scheduleMapper.findPage(q, (long) (page - 1) * size, size), total, page, size);
    }

    @Override
    public List<ClubScheduleResponse> clubSchedules(Long clubId) {
        LocalDate now = timeService.now().toLocalDate();
        List<ClubScheduleResponse> rows = scheduleMapper.findConfirmedForClub(clubId);
        rows.forEach(r -> r.setDaysUntilMatch(ChronoUnit.DAYS.between(now, r.getMatchDateTime().toLocalDate())));
        return rows;
    }

    private ScheduleDetailResponse detail(SeasonScheduleBatch batch) {
        ScheduleDetailResponse out = new ScheduleDetailResponse();
        BeanUtils.copyProperties(batch, out);
        Map<Integer, List<ScheduleMatchResponse>> grouped = new LinkedHashMap<>();
        for (ScheduleMatchResponse m : scheduleMapper.findBatchMatches(batch.getBatchId()))
            grouped.computeIfAbsent(m.getRoundNo(), k -> new ArrayList<>()).add(m);
        out.setRounds(
                grouped.entrySet().stream().map(e -> new ScheduleRoundResponse(e.getKey(), e.getValue())).toList());
        return out;
    }

    private void validateReusableRound(RoundInfo round, LocalDate start, LocalDate end) {
        if (!"DRAFT".equals(round.getRoundStatus()) || round.getStartDate() == null || round.getEndDate() == null
                || start.isBefore(round.getStartDate()) || end.isAfter(round.getEndDate()))
            throw conflict("现有轮次无法安全复用于自动排赛: 第" + round.getRoundNo() + "轮");
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
