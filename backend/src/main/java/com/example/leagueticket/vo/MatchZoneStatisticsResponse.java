package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MatchZoneStatisticsResponse {
    private Long matchZoneId;
    private String zoneName;
    private BigDecimal ticketPrice;
    private long totalSeatCount;
    private long validSoldCount;
    private long refundedCount;
    private BigDecimal ticketSaleRate;
}
