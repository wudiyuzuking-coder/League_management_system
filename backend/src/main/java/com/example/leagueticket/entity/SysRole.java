package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String roleStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
