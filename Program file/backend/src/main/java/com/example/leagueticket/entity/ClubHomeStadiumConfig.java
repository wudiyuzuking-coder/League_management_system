package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClubHomeStadiumConfig {
    private Long clubId;
    private Long stadiumId;
    private Integer rowsPerZone;
    private Integer longSideSeatsPerRow;
    private Integer shortSideSeatsPerRow;
    private BigDecimal vipPrice;
    private BigDecimal normalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
