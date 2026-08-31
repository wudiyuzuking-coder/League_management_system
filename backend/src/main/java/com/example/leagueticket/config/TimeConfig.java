package com.example.leagueticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("dev")
public class TimeConfig {
    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
