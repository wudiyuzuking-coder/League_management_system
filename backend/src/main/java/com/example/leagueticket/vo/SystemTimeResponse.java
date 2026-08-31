package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record SystemTimeResponse(LocalDateTime systemTime,LocalDateTime realTime,long offsetSeconds) {}
