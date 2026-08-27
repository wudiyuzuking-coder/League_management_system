package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper {

    @Select("SELECT role_id, permission_id, created_at FROM sys_role_permission WHERE role_id = #{roleId}")
    List<SysRolePermission> findByRoleId(Long roleId);

    @Select("""
            SELECT p.permission_code
            FROM sys_role_permission rp
            JOIN sys_permission p ON p.permission_id = rp.permission_id
            WHERE rp.role_id = #{roleId} AND p.permission_status = 'ENABLED'
            ORDER BY p.permission_code
            """)
    List<String> findPermissionCodesByRoleId(Long roleId);
}
