package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundApply {
    private Long refundId;
    private String refundNo;
    private Long orderId;
    private Long applicantId;
    private String reason;
    private BigDecimal refundAmount;
    private String refundStatus;
    private Long auditorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String orderNo;
    private String username;
    private Long matchId;
    private String homeClubName;
    private String awayClubName;
    private LocalDateTime matchTime;
    private String stadiumName;
    private String zoneName;
}
