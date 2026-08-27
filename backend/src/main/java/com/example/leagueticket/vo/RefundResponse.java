package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RefundResponse(Long refundId,String refundNo,Long orderId,String orderNo,Long applicantId,
        String username,BigDecimal refundAmount,String reason,String refundStatus,LocalDateTime appliedAt,
        Long auditorId,LocalDateTime auditedAt,String auditReason,Long matchId,String homeClubName,
        String awayClubName,LocalDateTime matchTime,String stadiumName,String zoneName,List<ETicketResponse> tickets) {}
