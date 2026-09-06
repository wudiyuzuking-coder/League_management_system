package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckinStatisticsResponse {
    private long totalAttempts;
    private long successCount;
    private long failedCount;
    private BigDecimal successRate;
    private long codeNotFoundCount;
    private long wrongMatchCount;
    private long orderInvalidCount;
    private long ticketUsedCount;
    private long ticketRefundedCount;
    private long ticketVoidCount;
}
