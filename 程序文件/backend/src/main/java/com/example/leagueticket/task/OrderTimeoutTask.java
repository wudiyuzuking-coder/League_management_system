package com.example.leagueticket.task;

import com.example.leagueticket.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j @Component @Profile("dev") @RequiredArgsConstructor
public class OrderTimeoutTask {
    private final OrderService orderService;
    @Scheduled(fixedDelay=60000,initialDelay=30000)
    public void closeExpiredOrders(){int count=orderService.closeExpiredBatch();if(count>0)log.info("Closed {} expired pending orders",count);}
}
