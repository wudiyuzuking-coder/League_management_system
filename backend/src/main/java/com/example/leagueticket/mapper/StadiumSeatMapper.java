package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.StadiumSeat;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StadiumSeatMapper {
    String COLUMNS="stadium_seat_id,stadium_id,stadium_zone_id,row_seq row_no,row_no row_label,seat_seq seat_no,seat_no seat_label,center_distance,seat_status";
    @Select("SELECT "+COLUMNS+" FROM stadium_seat WHERE stadium_zone_id=#{zoneId} ORDER BY row_seq,seat_seq") List<StadiumSeat> findByZone(Long zoneId);
    @Select("SELECT "+COLUMNS+" FROM stadium_seat WHERE stadium_seat_id=#{id}") StadiumSeat findById(Long id);
    @Select("SELECT COUNT(*) FROM stadium_seat WHERE stadium_zone_id=#{zoneId} AND ((row_seq=#{rowNo} AND seat_seq=#{seatNo}) OR (row_no=#{rowLabel} AND seat_no=#{seatLabel})) AND (#{excludeId} IS NULL OR stadium_seat_id!=#{excludeId})")
    int countConflict(@Param("zoneId") Long zoneId,@Param("rowNo") Integer rowNo,@Param("seatNo") Integer seatNo,@Param("rowLabel") String rowLabel,@Param("seatLabel") String seatLabel,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(#{stadiumId},#{stadiumZoneId},#{rowLabel},#{rowNo},#{seatLabel},#{seatNo},#{centerDistance},'ACTIVE')") @Options(useGeneratedKeys=true,keyProperty="stadiumSeatId") int insert(StadiumSeat seat);
    @Update("UPDATE stadium_seat SET row_no=#{rowLabel},row_seq=#{rowNo},seat_no=#{seatLabel},seat_seq=#{seatNo},center_distance=#{centerDistance} WHERE stadium_seat_id=#{stadiumSeatId}") int update(StadiumSeat seat);
    @Update("UPDATE stadium_seat SET seat_status=#{status} WHERE stadium_seat_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
    @Select("SELECT COUNT(*) FROM stadium_seat WHERE stadium_id=#{stadiumId}") long countTotal(Long stadiumId);
    @Select("SELECT COUNT(*) FROM stadium_seat WHERE stadium_id=#{stadiumId} AND seat_status=#{status}") long countStatus(@Param("stadiumId") Long stadiumId,@Param("status") String status);
}
