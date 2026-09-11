package com.example.leagueticket.vo;

import java.util.List;

public record PersonnelOverviewResponse(
        boolean compliant,String message,String reason,boolean standardHomeComplete,
        int starterCount,int substituteCount,int activeCoachCount,
        List<Player> players,List<Coach> coaches) {
    public record Player(Long playerId,String playerName,Integer birthYear,Integer age,String nationality,
                         String position,String lineupRole,String status,Integer shirtNo) {}
    public record Coach(Long coachId,String coachName,Integer birthYear,Integer age,String nationality,
                        String title,String status) {}
}
