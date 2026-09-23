package com.example.leagueticket.service.impl;

import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubSeasonEnrollmentMapper;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.MatchResultMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.mapper.SeasonScheduleMapper;
import com.example.leagueticket.service.SeasonLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SeasonLifecycleServiceImpl implements SeasonLifecycleService {
    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final ClubSeasonEnrollmentMapper enrollmentMapper;
    private final MatchInfoMapper matchMapper;
    private final MatchResultMapper resultMapper;

    @Override
    @Transactional
    public SeasonInfo openRegistration(Long seasonId) {
        SeasonInfo season = locked(seasonId);
        requireStatus(season, SeasonStatus.DRAFT);
        requireComplete(season);
        if (scheduleMapper.findBySeasonForUpdate(seasonId) != null) {
            throw conflict("已有排赛批次，不能开启报名");
        }
        return update(seasonId, SeasonStatus.REGISTRATION);
    }

    @Override
    @Transactional
    public SeasonInfo closeRegistration(Long seasonId) {
        SeasonInfo season = locked(seasonId);
        requireStatus(season, SeasonStatus.REGISTRATION);
        enrollmentMapper.findSubmittedIdsForUpdate(seasonId);
        return update(seasonId, SeasonStatus.PREPARING);
    }

    @Override
    @Transactional
    public SeasonInfo startInProgress(Long seasonId) {
        SeasonInfo season = locked(seasonId);
        requireStatus(season, SeasonStatus.PREPARING);
        SeasonScheduleBatch batch = scheduleMapper.findBySeasonForUpdate(seasonId);
        if (batch == null || !"CONFIRMED".equals(batch.getBatchStatus())) {
            throw conflict("赛程尚未确认，不能进入进行中");
        }
        if (scheduleMapper.countUnpublishedScheduledMatches(seasonId) > 0) {
            throw conflict("赛程中的比赛尚未全部发布");
        }
        return update(seasonId, SeasonStatus.IN_PROGRESS);
    }

    @Override
    @Transactional
    public SeasonInfo finish(Long seasonId) {
        SeasonInfo season = locked(seasonId);
        requireStatus(season, SeasonStatus.IN_PROGRESS);
        if (matchMapper.countBySeason(seasonId) == 0 || matchMapper.countNotFinishedBySeason(seasonId) > 0) {
            throw conflict("所有比赛完成后才能结束赛季");
        }
        if (resultMapper.countPendingBySeason(seasonId) > 0) {
            throw conflict("仍有待处理赛果，不能结束赛季");
        }
        return update(seasonId, SeasonStatus.FINISHED);
    }

    @Override
    @Transactional
    public SeasonInfo transitionCompatible(Long seasonId, String requestedStatus) {
        return switch (SeasonStatus.fromStored(requestedStatus)) {
            case REGISTRATION -> openRegistration(seasonId);
            case PREPARING -> closeRegistration(seasonId);
            case IN_PROGRESS -> startInProgress(seasonId);
            case FINISHED -> finish(seasonId);
            case DRAFT -> throw new BusinessException("赛季状态不允许回退为草稿");
        };
    }

    private SeasonInfo locked(Long seasonId) {
        SeasonInfo season = seasonMapper.findByIdForUpdate(seasonId);
        if (season == null) throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        return season;
    }

    private void requireStatus(SeasonInfo season, SeasonStatus expected) {
        SeasonStatus actual = SeasonStatus.fromStored(season.getSeasonStatus());
        if (actual != expected) {
            throw conflict("invalid season status transition: " + actual + " -> " + expected);
        }
    }

    private void requireComplete(SeasonInfo season) {
        if (season.getSeasonName() == null || season.getSeasonName().isBlank()
                || season.getStartDate() == null || season.getEndDate() == null
                || season.getRegistrationStartTime() == null || season.getRegistrationDeadline() == null
                || season.getMaxClubs() == null) {
            throw conflict("赛季资料不完整，不能开启报名");
        }
    }

    private SeasonInfo update(Long seasonId, SeasonStatus status) {
        seasonMapper.updateStatus(seasonId, status.name());
        return seasonMapper.findById(seasonId);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
