package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} LIMIT 1")
    SysRole findByCode(String roleCode);

    @Select("SELECT * FROM sys_role WHERE role_id = #{roleId} LIMIT 1")
    SysRole findById(Long roleId);

    @Select("SELECT * FROM sys_role ORDER BY role_id")
    List<SysRole> findAll();
}
