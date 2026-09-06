package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record CheckerMatchResponse(Long matchId, Long homeClubId, String homeClubName,
        String awayClubName, String stadiumName, LocalDateTime matchTime, String matchStatus,
        boolean checkinAvailable) {
}
