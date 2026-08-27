package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClubSeasonRecord {
    private Long recordId;
    private Long seasonId;
    private Long clubId;
    private Integer played;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer points;
    private Integer ranking;
    private LocalDateTime updatedAt;
}
