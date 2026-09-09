package com.example.leagueticket.service;

import com.example.leagueticket.entity.SysRole;

import java.util.List;

public interface SysRoleService {
    SysRole getByCode(String roleCode);
    List<SysRole> listAll();
}
