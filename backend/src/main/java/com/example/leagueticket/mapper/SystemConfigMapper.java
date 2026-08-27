package com.example.leagueticket.mapper;
import org.apache.ibatis.annotations.*;
@Mapper
public interface SystemConfigMapper {
    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key} AND config_status='ENABLED'") String findEnabledValue(String key);
}
