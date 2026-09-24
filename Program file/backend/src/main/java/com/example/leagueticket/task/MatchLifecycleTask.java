package com.example.leagueticket.task;

import com.example.leagueticket.service.MatchInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MatchLifecycleTask {
    private final MatchInfoService matchService;

    @Scheduled(cron = "0 * * * * *")
    public void startPublishedMatches() {
        int changed = matchService.startPublishedMatchesDue();
        if (changed > 0) log.info("自动开始到时比赛，count={}", changed);
    }
}
