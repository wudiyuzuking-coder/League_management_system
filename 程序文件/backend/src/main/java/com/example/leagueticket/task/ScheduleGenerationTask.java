package com.example.leagueticket.task;

import com.example.leagueticket.mapper.SeasonScheduleMapper;
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
    private final SeasonScheduleMapper mapper;
    private final SeasonScheduleService service;
    private final SystemTimeService timeService;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void afterEnrollment(ScheduleEligibilityEvent event){try{service.generateIfEligible(event.seasonId(),"FULL");}catch(Exception e){log.info("报名完成后赛程暂未生成，seasonId={}, reason={}",event.seasonId(),e.getMessage());}}

    @Scheduled(cron="0 * * * * *")
    public void deadlineScan(){for(Long seasonId:mapper.findDeadlineCandidates(timeService.now()))try{service.generateIfEligible(seasonId,"DEADLINE");}catch(Exception e){log.warn("截止扫描未能生成赛程，seasonId={}, reason={}",seasonId,e.getMessage());}}
}
