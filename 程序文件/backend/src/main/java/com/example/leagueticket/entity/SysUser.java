package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long userId;
    private String username;
    private String phone;
    private String passwordHash;
    private String realName;
    private String clubApplyName;
    private String employeeNo;
    private String avatarUrl;
    private Long roleId;
    private String roleCode;
    private Long clubId;
    private String userStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
