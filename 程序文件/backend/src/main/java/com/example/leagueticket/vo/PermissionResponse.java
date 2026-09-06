package com.example.leagueticket.vo;

import com.example.leagueticket.entity.SysPermission;

public record PermissionResponse(Long permissionId, String permissionCode, String permissionName,
                                 String permissionStatus, String description) {
    public static PermissionResponse from(SysPermission permission) {
        return new PermissionResponse(permission.getPermissionId(), permission.getPermissionCode(),
                permission.getPermissionName(), permission.getPermissionStatus(), permission.getDescription());
    }
}
