package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundStatisticsResponse {
    private long totalApplications;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private BigDecimal approvedRefundAmount;
    private long successfulPaidOrders;
    private BigDecimal refundRate;
}
