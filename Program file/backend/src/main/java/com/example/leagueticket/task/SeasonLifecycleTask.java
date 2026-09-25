package com.example.leagueticket.task;

import com.example.leagueticket.service.LifecycleCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SeasonLifecycleTask {
    private final LifecycleCompensationService compensationService;

    @Scheduled(cron = "0 * * * * *")
    public void advanceSeasons() {
        int opened = compensationService.openDueRegistrations();
        int started = compensationService.startDueSeasons();
        if (opened > 0) log.info("自动开启到期赛季报名，count={}", opened);
        if (started > 0) log.info("自动开始到期赛季，count={}", started);
    }
}
