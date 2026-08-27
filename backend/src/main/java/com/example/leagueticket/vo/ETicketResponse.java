package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ETicketResponse(Long ticketId,String ticketCode,Long orderId,Long itemId,String ticketStatus,
        LocalDateTime issuedAt,LocalDateTime enterTime,Long matchId,String homeClubName,String awayClubName,
        LocalDateTime matchTime,String stadiumName,String zoneName,String rowNo,String seatNo,BigDecimal ticketPrice) {}
