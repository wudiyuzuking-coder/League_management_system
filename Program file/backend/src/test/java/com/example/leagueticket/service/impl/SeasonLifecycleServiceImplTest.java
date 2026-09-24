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
import com.example.leagueticket.service.SystemTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SeasonLifecycleServiceImplTest {
    private SeasonInfoMapper seasons;
    private SeasonScheduleMapper schedules;
    private ClubSeasonEnrollmentMapper enrollments;
    private MatchInfoMapper matches;
    private MatchResultMapper results;
    private SystemTimeService time;
    private SeasonLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        seasons = mock(SeasonInfoMapper.class);
        schedules = mock(SeasonScheduleMapper.class);
        enrollments = mock(ClubSeasonEnrollmentMapper.class);
        matches = mock(MatchInfoMapper.class);
        results = mock(MatchResultMapper.class);
        time = mock(SystemTimeService.class);
        when(time.now()).thenReturn(LocalDateTime.of(2035, 1, 1, 0, 0));
        service = new SeasonLifecycleServiceImpl(seasons, schedules, enrollments, matches, results, time);
    }

    @Test
    void normalizesLegacyRequestAliasesAtTheBoundary() {
        assertThat(SeasonStatus.fromStored("REGISTERING")).isEqualTo(SeasonStatus.REGISTRATION);
        assertThat(SeasonStatus.fromStored("ACTIVE")).isEqualTo(SeasonStatus.IN_PROGRESS);
    }

    @Test
    void supportsTheCompleteLifecycle() {
        stubTransition(SeasonStatus.DRAFT, SeasonStatus.REGISTRATION);
        assertThat(service.openRegistration(1L).getSeasonStatus()).isEqualTo("REGISTRATION");
        verify(seasons).updateStatus(1L, "REGISTRATION");

        reset(seasons, schedules, enrollments, matches, results);
        stubTransition(SeasonStatus.REGISTRATION, SeasonStatus.PREPARING);
        assertThat(service.closeRegistration(1L).getSeasonStatus()).isEqualTo("PREPARING");
        verify(enrollments).findSubmittedIdsForUpdate(1L);

        reset(seasons, schedules, enrollments, matches, results);
        stubTransition(SeasonStatus.PREPARING, SeasonStatus.IN_PROGRESS);
        SeasonScheduleBatch batch = new SeasonScheduleBatch();
        batch.setBatchStatus("CONFIRMED");
        when(schedules.findBySeasonForUpdate(1L)).thenReturn(batch);
        when(schedules.countSeasonMatches(1L)).thenReturn(1);
        when(schedules.countUnpublishedScheduledMatches(1L)).thenReturn(0);
        assertThat(service.startInProgress(1L).getSeasonStatus()).isEqualTo("IN_PROGRESS");

        reset(seasons, schedules, enrollments, matches, results);
        stubTransition(SeasonStatus.IN_PROGRESS, SeasonStatus.FINISHED);
        when(matches.countBySeason(1L)).thenReturn(2);
        when(matches.countNotFinishedBySeason(1L)).thenReturn(0);
        when(results.countPendingBySeason(1L)).thenReturn(0);
        assertThat(service.finish(1L).getSeasonStatus()).isEqualTo("FINISHED");
    }

    @Test
    void rejectsIllegalJumpsAndLegacyAliasesCannotBypassGuards() {
        when(seasons.findByIdForUpdate(1L)).thenReturn(season(SeasonStatus.DRAFT));
        assertThatThrownBy(() -> service.transitionCompatible(1L, "ACTIVE"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("赛季将在开始日由系统自动进入进行中");

        when(seasons.findByIdForUpdate(1L)).thenReturn(season(SeasonStatus.REGISTRATION));
        assertThatThrownBy(() -> service.transitionCompatible(1L, "FINISHED"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REGISTRATION -> IN_PROGRESS");
        verify(seasons, never()).updateStatus(1L, "FINISHED");
    }

    @Test
    void compatibleStatusEndpointCannotCreateHalfPreparedSeason() {
        assertThatThrownBy(() -> service.transitionCompatible(1L, "PREPARING"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请使用关闭报名接口完成自动排赛与发布");
        verifyNoInteractions(seasons, schedules, enrollments, matches, results);
    }

    @Test
    void compatibleStatusEndpointCannotManuallyStartSeason() {
        assertThatThrownBy(() -> service.transitionCompatible(1L, "IN_PROGRESS"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("赛季将在开始日由系统自动进入进行中");
        verifyNoInteractions(seasons, schedules, enrollments, matches, results);
    }

    @Test
    void preparingCannotStartWithoutConfirmedPublishedSchedule() {
        when(seasons.findByIdForUpdate(1L)).thenReturn(season(SeasonStatus.PREPARING));
        SeasonScheduleBatch generated = new SeasonScheduleBatch();
        generated.setBatchStatus("GENERATED");
        when(schedules.findBySeasonForUpdate(1L)).thenReturn(generated);
        assertThatThrownBy(() -> service.startInProgress(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("赛程尚未确认，不能进入进行中");
    }

    @Test
    void automaticTransitionsRespectExactTimeAndRemainIdempotent() {
        SeasonInfo draft = season(SeasonStatus.DRAFT);
        when(seasons.findByIdForUpdate(1L)).thenReturn(draft);
        when(time.now()).thenReturn(draft.getRegistrationStartTime().minusMinutes(1));
        assertThat(service.openRegistrationIfDue(1L)).isFalse();
        verify(seasons, never()).updateStatus(anyLong(), anyString());

        when(time.now()).thenReturn(draft.getRegistrationStartTime());
        when(seasons.findById(1L)).thenReturn(season(SeasonStatus.REGISTRATION));
        assertThat(service.openRegistrationIfDue(1L)).isTrue();
        verify(seasons).updateStatus(1L, "REGISTRATION");

        when(seasons.findByIdForUpdate(1L)).thenReturn(season(SeasonStatus.REGISTRATION));
        assertThat(service.openRegistrationIfDue(1L)).isFalse();
    }

    @Test
    void seasonCannotStartBeforeStartDate() {
        SeasonInfo preparing = season(SeasonStatus.PREPARING);
        when(seasons.findByIdForUpdate(1L)).thenReturn(preparing);
        when(time.now()).thenReturn(preparing.getStartDate().atStartOfDay().minusMinutes(1));
        assertThat(service.startInProgressIfDue(1L)).isFalse();
        assertThatThrownBy(() -> service.startInProgress(1L))
                .hasMessage("赛季开始日尚未到达，不能进入进行中");
        verify(seasons, never()).updateStatus(1L, "IN_PROGRESS");
    }

    @Test
    void inProgressCannotFinishWithUnfinishedMatchesOrPendingResults() {
        when(seasons.findByIdForUpdate(1L)).thenReturn(season(SeasonStatus.IN_PROGRESS));
        when(matches.countBySeason(1L)).thenReturn(2);
        when(matches.countNotFinishedBySeason(1L)).thenReturn(1);
        assertThatThrownBy(() -> service.finish(1L)).hasMessage("所有比赛完成后才能结束赛季");

        when(matches.countNotFinishedBySeason(1L)).thenReturn(0);
        when(results.countPendingBySeason(1L)).thenReturn(1);
        assertThatThrownBy(() -> service.finish(1L)).hasMessage("仍有待处理赛果，不能结束赛季");
    }

    private void stubTransition(SeasonStatus from, SeasonStatus to) {
        when(seasons.findByIdForUpdate(1L)).thenReturn(season(from));
        when(seasons.findById(1L)).thenReturn(season(to));
    }

    private SeasonInfo season(SeasonStatus status) {
        SeasonInfo season = new SeasonInfo();
        season.setSeasonId(1L);
        season.setSeasonName("测试赛季");
        season.setStartDate(LocalDate.of(2035, 1, 1));
        season.setEndDate(LocalDate.of(2035, 6, 1));
        season.setRegistrationStartTime(LocalDateTime.of(2034, 10, 1, 20, 0));
        season.setRegistrationDeadline(LocalDateTime.of(2034, 12, 15, 19, 59));
        season.setMaxClubs(4);
        season.setSeasonStatus(status.name());
        return season;
    }
}
