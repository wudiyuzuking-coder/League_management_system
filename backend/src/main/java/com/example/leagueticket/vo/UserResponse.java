package com.example.leagueticket.vo;

import com.example.leagueticket.entity.SysUser;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String username,
        String phone,
        String realName,
        String employeeNo,
        String avatarUrl,
        String roleCode,
        Long clubId,
        String userStatus,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserResponse from(SysUser user) {
        return new UserResponse(user.getUserId(), user.getUsername(), user.getPhone(), user.getRealName(),
                user.getEmployeeNo(), user.getAvatarUrl(), user.getRoleCode(), user.getClubId(), user.getUserStatus(),
                user.getLastLoginAt(), user.getCreatedAt());
    }
}
