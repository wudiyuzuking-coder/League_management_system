package com.example.leagueticket.vo;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String username,
        String phone,
        String realName,
        String roleCode,
        Long clubId,
        List<String> permissions
) {
}
