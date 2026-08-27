package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchTicketZone {
    private Long matchZoneId;
    private Long matchId;
    private Long stadiumZoneId;
    private Long createdBy;
    private String zoneNameSnapshot;
    private String zoneCode;
    private BigDecimal ticketPrice;
    private String zoneStatus;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
