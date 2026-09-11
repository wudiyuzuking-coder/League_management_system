package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.ClubHomeStadiumConfig;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ClubHomeStadiumConfigMapper {
    String COLUMNS="club_id,stadium_id,rows_per_zone,long_side_seats_per_row,short_side_seats_per_row,vip_price,normal_price,created_at,updated_at";
    @Select("SELECT "+COLUMNS+" FROM club_home_stadium_config WHERE club_id=#{clubId}")
    ClubHomeStadiumConfig findByClubId(Long clubId);
    @Select("SELECT "+COLUMNS+" FROM club_home_stadium_config WHERE stadium_id=#{stadiumId}")
    ClubHomeStadiumConfig findByStadiumId(Long stadiumId);
    @Insert("INSERT INTO club_home_stadium_config(club_id,stadium_id,rows_per_zone,long_side_seats_per_row,short_side_seats_per_row,vip_price,normal_price) VALUES(#{clubId},#{stadiumId},#{rowsPerZone},#{longSideSeatsPerRow},#{shortSideSeatsPerRow},#{vipPrice},#{normalPrice})")
    int insert(ClubHomeStadiumConfig value);
    @Update("UPDATE club_home_stadium_config SET vip_price=#{vipPrice},normal_price=#{normalPrice} WHERE club_id=#{clubId}")
    int updatePrices(ClubHomeStadiumConfig value);
}
