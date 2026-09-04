package com.example.leagueticket.vo;

import com.example.leagueticket.entity.SysRole;

public record RoleResponse(Long roleId, String roleCode, String roleName, String roleStatus, String remark) {
    public static RoleResponse from(SysRole role) {
        return new RoleResponse(role.getRoleId(), role.getRoleCode(), role.getRoleName(), role.getRoleStatus(), role.getRemark());
    }
}
