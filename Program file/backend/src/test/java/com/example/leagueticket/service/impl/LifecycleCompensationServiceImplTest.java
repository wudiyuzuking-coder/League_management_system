package com.example.leagueticket.service.impl;

import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.mapper.SeasonScheduleMapper;
import com.example.leagueticket.mapper.TicketOrderMapper;
import com.example.leagueticket.service.MatchInfoService;
import com.example.leagueticket.service.OrderService;
import com.example.leagueticket.service.SeasonLifecycleService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.service.SystemTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LifecycleCompensationServiceImplTest {
    private final SeasonInfoMapper seasons = mock(SeasonInfoMapper.class);
    private final SeasonScheduleMapper schedules = mock(SeasonScheduleMapper.class);
    private final MatchInfoMapper matches = mock(MatchInfoMapper.class);
    private final TicketOrderMapper orders = mock(TicketOrderMapper.class);
    private final SeasonLifecycleService seasonLifecycle = mock(SeasonLifecycleService.class);
    private final SeasonScheduleService seasonSchedule = mock(SeasonScheduleService.class);
    private final MatchInfoService matchService = mock(MatchInfoService.class);
    private final OrderService orderService = mock(OrderService.class);
    private final SystemTimeService time = mock(SystemTimeService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final LocalDateTime now = LocalDateTime.of(2048, 11, 15, 20, 0);
    private LifecycleCompensationServiceImpl service;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(now);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenAnswer(invocation -> new SimpleTransactionStatus());
        service = new LifecycleCompensationServiceImpl(seasons, schedules, matches, orders, seasonLifecycle,
                seasonSchedule, matchService, orderService, time, transactions);
    }

    @Test
    void catchUpRunsAllStagesInBusinessOrder() {
        when(seasons.findDraftRegistrationCandidates(now)).thenReturn(List.of(1L));
        when(schedules.findDeadlineCandidates(now, SeasonStatus.REGISTRATION)).thenReturn(List.of(2L));
        when(seasons.findPreparingStartCandidates(now.toLocalDate())).thenReturn(List.of(3L));
        when(matches.findPublishedStartCandidates(now)).thenReturn(List.of(4L));
        when(orders.findAllExpiredIds(now)).thenReturn(List.of(5L));
        when(seasonLifecycle.openRegistrationIfDue(1L)).thenReturn(true);
        when(seasonLifecycle.startInProgressIfDue(3L)).thenReturn(true);
        when(matchService.startPublishedMatchIfDue(4L)).thenReturn(true);
        when(orderService.closeExpiredOrder(5L)).thenReturn(true);

        service.catchUpAfterSystemTimeChange();

        var ordered = inOrder(seasonLifecycle, seasonSchedule, matchService, orderService);
        ordered.verify(seasonLifecycle).openRegistrationIfDue(1L);
        ordered.verify(seasonSchedule).generateIfEligible(2L, "DEADLINE");
        ordered.verify(seasonLifecycle).startInProgressIfDue(3L);
        ordered.verify(matchService).startPublishedMatchIfDue(4L);
        ordered.verify(orderService).closeExpiredOrder(5L);
    }

    @Test
    void oneFailedEntityDoesNotBlockTheNextCandidate() {
        when(matches.findPublishedStartCandidates(now)).thenReturn(List.of(10L, 11L));
        when(matchService.startPublishedMatchIfDue(10L)).thenThrow(new IllegalStateException("broken match"));
        when(matchService.startPublishedMatchIfDue(11L)).thenReturn(true);

        assertThat(service.startDueMatches()).isEqualTo(1);
        verify(matchService).startPublishedMatchIfDue(11L);
        verify(transactions, atLeastOnce()).rollback(any());
    }
}
