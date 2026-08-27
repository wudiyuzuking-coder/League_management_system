package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClubStatisticsResponse {
    private Long clubId;
    private String clubName;
    private long homeMatchCount;
    private long validTicketsSold;
    private long checkedInTickets;
    private BigDecimal grossSalesAmount;
    private BigDecimal refundAmount;
    private BigDecimal netSalesAmount;
    private BigDecimal averageTicketSaleRate;
    private BigDecimal averageAttendanceRate;
}
