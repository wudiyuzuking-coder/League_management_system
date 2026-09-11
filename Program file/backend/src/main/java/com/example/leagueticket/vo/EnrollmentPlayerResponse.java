package com.example.leagueticket.vo;

public record EnrollmentPlayerResponse(Long playerId,String playerName,Integer shirtNo,String position,
        String lineupRole,Integer birthYear,Integer age,String nationality) {}
