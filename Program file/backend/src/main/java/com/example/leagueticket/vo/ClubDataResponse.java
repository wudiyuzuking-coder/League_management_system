package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.util.List;

public record ClubDataResponse(BigDecimal revenue,ClubPerformanceResponse performance,List<ClubPerformanceResponse> rankings) {}
