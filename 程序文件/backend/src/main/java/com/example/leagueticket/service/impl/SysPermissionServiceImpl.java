package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.SysPermission;
import com.example.leagueticket.mapper.SysPermissionMapper;
import com.example.leagueticket.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {
    private final SysPermissionMapper permissionMapper;

    @Override
    public List<SysPermission> listAll() {
        return permissionMapper.findAll();
    }
}
