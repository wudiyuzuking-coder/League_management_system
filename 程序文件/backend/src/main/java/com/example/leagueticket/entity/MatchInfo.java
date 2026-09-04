package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MatchInfo {
    private Long matchId;
    private Long seasonId;
    private String seasonName;
    private Long roundId;
    private Integer roundNo;
    private String roundName;
    private Long homeClubId;
    private String homeClubName;
    private String homeLogoUrl;
    private Long awayClubId;
    private String awayClubName;
    private String awayLogoUrl;
    private Long stadiumId;
    private String stadiumName;
    private LocalDateTime matchTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer homeScore;
    private Integer awayScore;
    private String matchStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
