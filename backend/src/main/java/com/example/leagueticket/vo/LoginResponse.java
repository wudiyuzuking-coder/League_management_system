package com.example.leagueticket.vo;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName,
        String roleCode,
        Long clubId
) {
}
