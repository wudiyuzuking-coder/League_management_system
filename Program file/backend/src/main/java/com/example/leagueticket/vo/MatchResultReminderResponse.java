package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record MatchResultReminderResponse(
        Long matchId,Long seasonId,String seasonName,Long roundId,Integer roundNo,String roundName,
        Long homeClubId,String homeClubName,Long awayClubId,String awayClubName,
        LocalDateTime matchTime,String matchStatus,Integer homeScore,Integer awayScore,
        String reminderType,long daysOverdue) {}
