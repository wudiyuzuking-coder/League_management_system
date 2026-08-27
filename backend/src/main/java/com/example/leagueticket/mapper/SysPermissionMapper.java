package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    @Select("SELECT * FROM sys_permission ORDER BY permission_id")
    List<SysPermission> findAll();
}
