package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ETicket {
    private Long ticketId;
    private String ticketCode;
    private Long orderId;
    private Long itemId;
    private String ticketStatus;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime updatedAt;
    private Long matchId;
    private String homeClubName;
    private String awayClubName;
    private LocalDateTime matchTime;
    private String stadiumName;
    private String zoneName;
    private String rowNo;
    private String seatNo;
    private BigDecimal ticketPrice;
}
