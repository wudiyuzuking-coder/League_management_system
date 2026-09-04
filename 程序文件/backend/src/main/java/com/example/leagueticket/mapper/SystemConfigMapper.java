package com.example.leagueticket.mapper;
import org.apache.ibatis.annotations.*;
@Mapper
public interface SystemConfigMapper {
    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key} AND config_status='ENABLED'") String findEnabledValue(String key);
    @Insert("INSERT IGNORE INTO sys_config(config_key,config_value,value_type,description,config_status) VALUES(#{key},'0','INTEGER',#{description},'ENABLED')")
    int ensureOffsetConfig(@Param("key")String key,@Param("description")String description);
    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key} FOR UPDATE") String findValueForUpdate(String key);
    @Update("UPDATE sys_config SET config_value=#{value},value_type='INTEGER',description=#{description},config_status='ENABLED' WHERE config_key=#{key}")
    int updateOffset(@Param("key")String key,@Param("value")String value,@Param("description")String description);
}
