package com.example.leagueticket.vo;
import java.math.BigDecimal;
public record SeasonRevenueResponse(Long seasonId,BigDecimal effectiveRevenue,BigDecimal platformShare){}
