package com.example.leagueticket.vo;

public record StandingResponse(Long recordId, Long clubId, String clubName, String logoUrl,
                               int matchesPlayed, int wins, int draws, int losses,
                               int goalsFor, int goalsAgainst, int goalDifference,
                               int points, int rank) {}
