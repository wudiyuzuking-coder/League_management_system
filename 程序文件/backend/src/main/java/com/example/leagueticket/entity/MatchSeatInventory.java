package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MatchSeatInventory {
    private Long inventoryId;
    private Long matchId;
    private Long matchZoneId;
    private Long stadiumSeatId;
    private String inventoryStatus;
    private Long lockOrderId;
    private LocalDateTime lockedAt;
    private LocalDateTime lockExpireTime;
    private Integer version;
    private Integer rowNo;
    private String rowLabel;
    private Integer seatNo;
    private String seatLabel;
    private Integer physicalMinSeatNo;
    private Integer physicalMaxSeatNo;
}
