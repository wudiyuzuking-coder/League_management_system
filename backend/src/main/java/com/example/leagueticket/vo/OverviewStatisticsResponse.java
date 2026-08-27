package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OverviewStatisticsResponse {
    private long totalMatches;
    private long totalOrders;
    private long paidOrders;
    private long refundedOrders;
    private long validTicketsSold;
    private long checkedInTickets;
    private BigDecimal grossSalesAmount;
    private BigDecimal refundAmount;
    private BigDecimal netSalesAmount;
    private BigDecimal averageAttendanceRate;
}
