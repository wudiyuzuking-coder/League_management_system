package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRolePermission {
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createdAt;
}
