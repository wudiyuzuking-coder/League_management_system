package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("application", applicationName);
        details.put("time", Instant.now());
        return Result.success(details);
    }
}
