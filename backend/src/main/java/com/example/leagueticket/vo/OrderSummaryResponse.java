package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long orderId,String orderNo,Long matchId,Long matchZoneId,
        String homeClubName,String awayClubName,LocalDateTime matchTime,String stadiumName,String zoneName,
        Integer ticketCount,BigDecimal totalAmount,String orderStatus,LocalDateTime expireTime,
        LocalDateTime paidAt,LocalDateTime cancelledAt,String cancelReason,LocalDateTime createdAt) {}
