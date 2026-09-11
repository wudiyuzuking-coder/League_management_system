package com.example.leagueticket.vo;

import lombok.Data;

@Data
public class ClubPerformanceResponse {
    private Long clubId; private String clubName; private String logoUrl;
    private long matchesPlayed; private long points; private long wins; private long draws; private long losses;
    private long goalsFor; private long goalsAgainst; private long goalDifference;
}
