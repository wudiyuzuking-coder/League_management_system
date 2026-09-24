package com.example.leagueticket.task;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.SeasonLifecycleService;
import com.example.leagueticket.service.SystemTimeService;
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
    private final SeasonInfoMapper seasonMapper;
    private final SeasonLifecycleService lifecycleService;
    private final SystemTimeService timeService;

    @Scheduled(cron = "0 * * * * *")
    public void advanceSeasons() {
        var now = timeService.now();
        for (Long seasonId : seasonMapper.findDraftRegistrationCandidates(now)) {
            try {
                lifecycleService.openRegistrationIfDue(seasonId);
            } catch (BusinessException e) {
                log.warn("自动开启赛季报名失败，seasonId={}, reason={}", seasonId, e.getMessage());
            } catch (Exception e) {
                log.warn("自动开启赛季报名失败，seasonId={}", seasonId, e);
            }
        }
        for (Long seasonId : seasonMapper.findPreparingStartCandidates(now.toLocalDate())) {
            try {
                lifecycleService.startInProgressIfDue(seasonId);
            } catch (BusinessException e) {
                log.warn("自动开始赛季失败，seasonId={}, reason={}", seasonId, e.getMessage());
            } catch (Exception e) {
                log.warn("自动开始赛季失败，seasonId={}", seasonId, e);
            }
        }
    }
}
