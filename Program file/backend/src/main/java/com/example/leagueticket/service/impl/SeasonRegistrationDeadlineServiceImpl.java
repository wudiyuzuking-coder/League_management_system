package com.example.leagueticket.service.impl;

import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubSeasonEnrollmentMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.mapper.SeasonScheduleMapper;
import com.example.leagueticket.service.ClubSeasonNotificationService;
import com.example.leagueticket.service.SeasonRegistrationDeadlineService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.service.SystemTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class SeasonRegistrationDeadlineServiceImpl implements SeasonRegistrationDeadlineService {
    public static final String INSUFFICIENT_CLUBS_REASON = "参赛俱乐部不足 2 支";

    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final ClubSeasonEnrollmentMapper enrollmentMapper;
    private final SeasonScheduleService scheduleService;
    private final ClubSeasonNotificationService notificationService;
    private final SystemTimeService timeService;

    @Override
    @Transactional
    public boolean processRegistrationDeadline(Long seasonId) {
        SeasonInfo season = seasonMapper.findByIdForUpdate(seasonId);
        if (season == null) throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        if (!SeasonStatus.REGISTRATION.matches(season.getSeasonStatus())) return false;

        LocalDateTime now = timeService.now();
        if (season.getRegistrationDeadline() == null || now.isBefore(season.getRegistrationDeadline())) return false;
        if (scheduleMapper.findBySeasonForUpdate(seasonId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "报名中的赛季已存在排赛批次");
        }

        List<Long> submitted = enrollmentMapper.findSubmittedIdsForUpdate(seasonId);
        if (submitted.size() >= 2) {
            scheduleService.generateIfEligible(seasonId, "DEADLINE");
            return true;
        }

        if (seasonMapper.cancelRegistration(seasonId, INSUFFICIENT_CLUBS_REASON, now) != 1) return false;
        int notifications = notificationService.createCancellationNotifications(seasonId, now);
        log.info("赛季因报名不足自动取消 seasonId={}, successfulEnrollmentCount={}, cancelledAt={}, notifications={}",
                seasonId, submitted.size(), now, notifications);
        return true;
    }
}
