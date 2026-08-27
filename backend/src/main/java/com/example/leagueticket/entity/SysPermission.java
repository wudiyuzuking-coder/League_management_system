package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {
    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String permissionStatus;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
