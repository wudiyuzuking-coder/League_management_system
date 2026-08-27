package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchStatisticsResponse {
    private Long matchId;
    private Long seasonId;
    private Long roundId;
    private Long homeClubId;
    private String homeClubName;
    private String awayClubName;
    private LocalDateTime matchTime;
    private String matchStatus;
    private long totalSeatCount;
    private long validSoldCount;
    private long refundedCount;
    private long checkedInCount;
    private BigDecimal grossSalesAmount;
    private BigDecimal refundAmount;
    private BigDecimal netSalesAmount;
    private BigDecimal ticketSaleRate;
    private BigDecimal attendanceRate;
    private long successCount;
    private long codeNotFoundCount;
    private long wrongMatchCount;
    private long orderInvalidCount;
    private long ticketUsedCount;
    private long ticketRefundedCount;
    private long ticketVoidCount;
    private List<MatchZoneStatisticsResponse> zones;
}
