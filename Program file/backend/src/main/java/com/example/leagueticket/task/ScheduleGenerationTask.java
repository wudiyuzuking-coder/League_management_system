package com.example.leagueticket.task;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j @Component @Profile("dev") @RequiredArgsConstructor
public class ScheduleGenerationTask {
    private final SeasonScheduleService service;
    private final LifecycleCompensationService compensationService;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void afterEnrollment(ScheduleEligibilityEvent event){
        try {
            service.generateIfEligible(event.seasonId(),"FULL");
        } catch (BusinessException e) {
            if ("报名未满额且报名尚未截止，暂不自动生成赛程".equals(e.getMessage())) {
                log.info("报名完成后暂不满足自动排赛条件，seasonId={}, reason={}",event.seasonId(),e.getMessage());
            } else {
                log.warn("报名完成后自动排赛失败，事务已回滚，seasonId={}",event.seasonId(),e);
            }
        } catch (Exception e) {
            log.warn("报名完成后自动排赛失败，事务已回滚，seasonId={}",event.seasonId(),e);
        }
    }

    @Scheduled(cron="0 * * * * *")
    public void deadlineScan(){int changed=compensationService.closeDueRegistrations();if(changed>0)log.info("截止扫描自动排赛完成，count={}",changed);}
}
