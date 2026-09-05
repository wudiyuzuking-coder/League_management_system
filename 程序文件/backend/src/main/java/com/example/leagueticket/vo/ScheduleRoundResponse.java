package com.example.leagueticket.vo;

import java.util.List;

public record ScheduleRoundResponse(Integer roundNo, List<ScheduleMatchResponse> matches) {}
