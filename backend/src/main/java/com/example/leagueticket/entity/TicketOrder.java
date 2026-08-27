package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketOrder {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long matchId;
    private Long matchZoneId;
    private Integer ticketCount;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime expireTime;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Integer version;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String homeClubName;
    private String awayClubName;
    private LocalDateTime matchTime;
    private String stadiumName;
    private String zoneName;
}
