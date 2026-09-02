package com.example.leagueticket.vo;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String username,
        String phone,
        String realName,
        String employeeNo,
        String avatarUrl,
        String roleCode,
        Long clubId,
        String userStatus,
        List<String> permissions
) {
}
