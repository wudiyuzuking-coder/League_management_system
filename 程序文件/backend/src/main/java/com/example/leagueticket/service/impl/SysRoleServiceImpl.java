package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.SysRole;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysRoleMapper;
import com.example.leagueticket.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {
    private final SysRoleMapper roleMapper;

    @Override
    public SysRole getByCode(String roleCode) {
        SysRole role = roleMapper.findByCode(roleCode);
        if (role == null || !"ENABLED".equals(role.getRoleStatus())) {
            throw new BusinessException("role does not exist or is disabled");
        }
        return role;
    }

    @Override
    public List<SysRole> listAll() {
        return roleMapper.findAll();
    }
}
