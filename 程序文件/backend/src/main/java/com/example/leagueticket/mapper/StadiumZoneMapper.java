package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.StadiumZone;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StadiumZoneMapper {
    String COLUMNS="stadium_zone_id,stadium_id,zone_code,zone_name,sort_order sort_no,zone_status,description";
    @Select("SELECT "+COLUMNS+" FROM stadium_zone WHERE stadium_id=#{stadiumId} ORDER BY sort_order,stadium_zone_id") List<StadiumZone> findByStadium(Long stadiumId);
    @Select("SELECT "+COLUMNS+" FROM stadium_zone WHERE stadium_zone_id=#{id}") StadiumZone findById(Long id);
    @Select("SELECT COUNT(*) FROM stadium_zone WHERE stadium_id=#{stadiumId} AND zone_code=#{code} AND (#{excludeId} IS NULL OR stadium_zone_id!=#{excludeId})") int countCode(@Param("stadiumId") Long stadiumId,@Param("code") String code,@Param("excludeId") Long excludeId);
    @Select("SELECT COUNT(*) FROM stadium_zone WHERE stadium_id=#{stadiumId} AND zone_name=#{name} AND (#{excludeId} IS NULL OR stadium_zone_id!=#{excludeId})") int countName(@Param("stadiumId") Long stadiumId,@Param("name") String name,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status,description) VALUES(#{stadiumId},#{zoneCode},#{zoneName},#{sortNo},'ACTIVE',#{description})") @Options(useGeneratedKeys=true,keyProperty="stadiumZoneId") int insert(StadiumZone zone);
    @Update("UPDATE stadium_zone SET zone_code=#{zoneCode},zone_name=#{zoneName},sort_order=#{sortNo},description=#{description} WHERE stadium_zone_id=#{stadiumZoneId}") int update(StadiumZone zone);
    @Update("UPDATE stadium_zone SET zone_status=#{status} WHERE stadium_zone_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
}
