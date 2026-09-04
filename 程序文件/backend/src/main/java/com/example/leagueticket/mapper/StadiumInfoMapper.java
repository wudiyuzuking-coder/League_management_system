package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.StadiumInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StadiumInfoMapper {
    String COLUMNS="stadium_id,stadium_name,city,address,capacity,layout_description layout_desc,stadium_status";
    @Select("SELECT "+COLUMNS+" FROM stadium_info ORDER BY stadium_id") List<StadiumInfo> findAll();
    @Select("<script>SELECT "+COLUMNS+" FROM stadium_info <where><if test='name!=null and name!=\"\"'>AND stadium_name LIKE CONCAT('%',#{name},'%')</if><if test='city!=null and city!=\"\"'>AND city LIKE CONCAT('%',#{city},'%')</if></where> ORDER BY stadium_id DESC</script>")
    List<StadiumInfo> search(@Param("name") String name,@Param("city") String city);
    @Select("SELECT "+COLUMNS+" FROM stadium_info WHERE stadium_id=#{id}") StadiumInfo findById(Long id);
    @Select("SELECT COUNT(*) FROM stadium_info WHERE stadium_name=#{name} AND city=#{city} AND (#{excludeId} IS NULL OR stadium_id!=#{excludeId})") int countDuplicate(@Param("name") String name,@Param("city") String city,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO stadium_info(stadium_name,city,address,capacity,layout_description,stadium_status) VALUES(#{stadiumName},#{city},#{address},#{capacity},#{layoutDesc},'ACTIVE')") @Options(useGeneratedKeys=true,keyProperty="stadiumId") int insert(StadiumInfo stadium);
    @Update("UPDATE stadium_info SET stadium_name=#{stadiumName},city=#{city},address=#{address},capacity=#{capacity},layout_description=#{layoutDesc} WHERE stadium_id=#{stadiumId}") int update(StadiumInfo stadium);
    @Update("UPDATE stadium_info SET stadium_status=#{status} WHERE stadium_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
}
