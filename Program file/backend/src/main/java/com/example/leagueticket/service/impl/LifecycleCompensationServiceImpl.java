package com.example.leagueticket.service.impl;

import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.mapper.SeasonScheduleMapper;
import com.example.leagueticket.mapper.TicketOrderMapper;
import com.example.leagueticket.service.LifecycleCompensationService;
import com.example.leagueticket.service.MatchInfoService;
import com.example.leagueticket.service.OrderService;
import com.example.leagueticket.service.SeasonLifecycleService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.service.SystemTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class LifecycleCompensationServiceImpl implements LifecycleCompensationService {
    private final SeasonInfoMapper seasonMapper;
    private final SeasonScheduleMapper scheduleMapper;
    private final MatchInfoMapper matchMapper;
    private final TicketOrderMapper orderMapper;
    private final SeasonLifecycleService seasonLifecycleService;
    private final SeasonScheduleService seasonScheduleService;
    private final MatchInfoService matchService;
    private final OrderService orderService;
    private final SystemTimeService timeService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void catchUpAfterSystemTimeChange() {
        LocalDateTime systemTime = timeService.now();
        log.info("生命周期补偿开始 systemTime={}", systemTime);
        int registrations = runStage("AUTO_REGISTRATION", systemTime, this::openDueRegistrations);
        int schedules = runStage("REGISTRATION_CLOSE", systemTime, this::closeDueRegistrations);
        int seasons = runStage("SEASON_START", systemTime, this::startDueSeasons);
        int matches = runStage("MATCH_START", systemTime, this::startDueMatches);
        int orders = runStage("ORDER_TIMEOUT", systemTime, this::closeExpiredOrders);
        log.info("生命周期补偿完成 systemTime={}, autoRegistration={}, registrationClose={}, seasonStart={}, matchStart={}, orderTimeout={}",
                systemTime, registrations, schedules, seasons, matches, orders);
    }

    @Override
    public int openDueRegistrations() {
        LocalDateTime now = timeService.now();
        List<Long> ids = requiresNew(() -> seasonMapper.findDraftRegistrationCandidates(now));
        int changed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(requiresNew(() -> seasonLifecycleService.openRegistrationIfDue(id)))) changed++;
            } catch (Exception exception) {
                logEntityFailure("AUTO_REGISTRATION", "season", id, now, exception);
            }
        }
        return changed;
    }

    @Override
    public int closeDueRegistrations() {
        LocalDateTime now = timeService.now();
        List<Long> ids = requiresNew(() -> scheduleMapper.findDeadlineCandidates(now, SeasonStatus.REGISTRATION));
        int changed = 0;
        for (Long id : ids) {
            try {
                requiresNew(() -> seasonScheduleService.generateIfEligible(id, "DEADLINE"));
                changed++;
            } catch (Exception exception) {
                logEntityFailure("REGISTRATION_CLOSE", "season", id, now, exception);
            }
        }
        return changed;
    }

    @Override
    public int startDueSeasons() {
        LocalDateTime now = timeService.now();
        List<Long> ids = requiresNew(() -> seasonMapper.findPreparingStartCandidates(now.toLocalDate()));
        int changed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(requiresNew(() -> seasonLifecycleService.startInProgressIfDue(id)))) changed++;
            } catch (Exception exception) {
                logEntityFailure("SEASON_START", "season", id, now, exception);
            }
        }
        return changed;
    }

    @Override
    public int startDueMatches() {
        LocalDateTime now = timeService.now();
        List<Long> ids = requiresNew(() -> matchMapper.findPublishedStartCandidates(now));
        int changed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(requiresNew(() -> matchService.startPublishedMatchIfDue(id)))) changed++;
            } catch (Exception exception) {
                logEntityFailure("MATCH_START", "match", id, now, exception);
            }
        }
        return changed;
    }

    @Override
    public int closeExpiredOrders() {
        LocalDateTime now = timeService.now();
        List<Long> ids = requiresNew(() -> orderMapper.findAllExpiredIds(now));
        int changed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(requiresNew(() -> orderService.closeExpiredOrder(id)))) changed++;
            } catch (Exception exception) {
                logEntityFailure("ORDER_TIMEOUT", "order", id, now, exception);
            }
        }
        return changed;
    }

    private int runStage(String stage, LocalDateTime systemTime, IntSupplier action) {
        try {
            return action.getAsInt();
        } catch (Exception exception) {
            log.error("生命周期补偿阶段失败 stage={}, entityId=ALL, systemTime={}", stage, systemTime, exception);
            return 0;
        }
    }

    private void logEntityFailure(String stage, String entityType, Long entityId,
                                  LocalDateTime systemTime, Exception exception) {
        log.warn("生命周期补偿对象失败 stage={}, entityType={}, entityId={}, systemTime={}",
                stage, entityType, entityId, systemTime, exception);
    }

    private <T> T requiresNew(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }
}
