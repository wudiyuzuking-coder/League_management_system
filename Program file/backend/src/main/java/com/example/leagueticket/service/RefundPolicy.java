package com.example.leagueticket.service;
import java.math.BigDecimal;import java.time.LocalDateTime;
public final class RefundPolicy {private RefundPolicy(){}public static BigDecimal rate(LocalDateTime now,LocalDateTime matchTime){return now.isAfter(matchTime.minusDays(7))?new BigDecimal("0.50"):BigDecimal.ONE;}}
