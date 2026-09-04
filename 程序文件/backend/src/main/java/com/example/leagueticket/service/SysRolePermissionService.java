package com.example.leagueticket.service;

import com.example.leagueticket.entity.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService {
    List<SysRolePermission> listByRoleId(Long roleId);
    List<String> listPermissionCodes(Long roleId);
}
