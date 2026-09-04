package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.SysRolePermission;
import com.example.leagueticket.mapper.SysRolePermissionMapper;
import com.example.leagueticket.service.SysRolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SysRolePermissionServiceImpl implements SysRolePermissionService {
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<SysRolePermission> listByRoleId(Long roleId) {
        return rolePermissionMapper.findByRoleId(roleId);
    }

    @Override
    public List<String> listPermissionCodes(Long roleId) {
        return rolePermissionMapper.findPermissionCodesByRoleId(roleId);
    }
}
