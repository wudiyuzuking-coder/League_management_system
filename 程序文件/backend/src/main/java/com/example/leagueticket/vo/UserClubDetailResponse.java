package com.example.leagueticket.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserClubDetailResponse(
        Club club,
        List<Player> players,
        List<Coach> coaches,
        Standing standing,
        List<Match> recentMatches,
        List<Match> upcomingMatches,
        Match nextMatch,
        Long daysUntilNextMatch) {

    public record Club(Long clubId, String clubName, String shortName, String logoUrl,
                       String homeCity, String description, Stadium homeStadium) {}

    public record Stadium(Long stadiumId, String stadiumName, String city, String address,
                          Integer capacity, String layoutDescription) {}

    public record Player(Long playerId, String name, Integer number, String position,
                         Integer age, String nationality) {}

    public record Coach(Long coachId, String name, String title, String nationality) {}

    public record Standing(Long seasonId, String seasonName, int matchesPlayed, int wins,
                           int draws, int losses, int goalsFor, int goalsAgainst,
                           int goalDifference, int points, int rank) {}

    public record Match(Long matchId, Long seasonId, String seasonName, Long homeClubId,
                        String homeClubName, Long awayClubId, String awayClubName,
                        LocalDateTime matchTime, Long stadiumId, String stadiumName,
                        Integer homeScore, Integer awayScore, String matchStatus) {}
}
