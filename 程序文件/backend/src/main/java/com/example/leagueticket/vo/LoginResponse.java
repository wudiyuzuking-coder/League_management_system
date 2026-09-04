package com.example.leagueticket.vo;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String phone,
        String realName,
        String roleCode,
        Long clubId
) {
}
