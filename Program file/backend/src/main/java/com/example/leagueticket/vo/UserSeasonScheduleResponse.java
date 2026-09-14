package com.example.leagueticket.vo;

import java.time.LocalDate;
import java.util.List;

public record UserSeasonScheduleResponse(
        Long seasonId,
        String seasonName,
        LocalDate startDate,
        LocalDate endDate,
        int clubCount,
        List<ScheduleRoundResponse> rounds) {}
