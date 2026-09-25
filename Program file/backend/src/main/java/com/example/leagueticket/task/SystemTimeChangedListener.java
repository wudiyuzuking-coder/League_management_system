package com.example.leagueticket.task;

import com.example.leagueticket.service.LifecycleCompensationService;
import com.example.leagueticket.service.SystemTimeChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SystemTimeChangedListener {
    private final LifecycleCompensationService compensationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterSystemTimeChanged(SystemTimeChangedEvent event) {
        try {
            compensationService.catchUpAfterSystemTimeChange();
        } catch (Exception exception) {
            log.error("系统时间已提交，但生命周期补偿发生未处理异常 stage=PIPELINE, entityId=ALL, systemTime={}, operationType={}",
                    event.systemTime(), event.operationType(), exception);
        }
    }
}
