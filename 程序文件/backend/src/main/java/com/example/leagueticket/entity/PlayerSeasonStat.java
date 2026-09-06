package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlayerSeasonStat {
    private Long statId;
    private Long seasonId;
    private String seasonName;
    private Long playerId;
    private String playerName;
    private Long clubId;
    private Integer appearances;
    private Integer starts;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private LocalDateTime updatedAt;
}
