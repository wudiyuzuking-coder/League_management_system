package com.example.leagueticket.config;

import com.example.leagueticket.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-password-init", name = "enabled", havingValue = "true")
public class DemoPasswordInitializer implements ApplicationRunner {

    private final SysUserService userService;

    @Value("${app.demo-password-init.password:123456}")
    private String demoPassword;

    @Override
    public void run(ApplicationArguments args) {
        int updated = userService.initializeDemoPasswords(demoPassword);
        log.info("Initialized BCrypt passwords for {} demo account(s)", updated);
    }
}
