package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.MatchTicketZone;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MatchTicketZoneMapper {
    String SELECT_COLUMNS="mz.match_zone_id,mz.match_id,mz.stadium_zone_id,mz.created_by,mz.zone_name_snapshot,sz.zone_code,mz.ticket_price,mz.zone_status,mz.sale_start_time,mz.sale_end_time,mz.version,mz.created_at,mz.updated_at";
    @Select("SELECT "+SELECT_COLUMNS+" FROM match_ticket_zone mz JOIN stadium_zone sz ON sz.stadium_zone_id=mz.stadium_zone_id WHERE mz.match_id=#{matchId} ORDER BY sz.sort_order,mz.match_zone_id")
    List<MatchTicketZone> findByMatch(Long matchId);
    @Select("SELECT "+SELECT_COLUMNS+" FROM match_ticket_zone mz JOIN stadium_zone sz ON sz.stadium_zone_id=mz.stadium_zone_id WHERE mz.match_zone_id=#{id}")
    MatchTicketZone findById(Long id);
    @Select("SELECT "+SELECT_COLUMNS+" FROM match_ticket_zone mz JOIN stadium_zone sz ON sz.stadium_zone_id=mz.stadium_zone_id WHERE mz.match_zone_id=#{id} FOR UPDATE")
    MatchTicketZone findByIdForUpdate(Long id);
    @Select("SELECT COUNT(*) FROM match_ticket_zone WHERE match_id=#{matchId} AND stadium_zone_id=#{stadiumZoneId} AND (#{excludeId} IS NULL OR match_zone_id!=#{excludeId})")
    int countDuplicate(@Param("matchId") Long matchId,@Param("stadiumZoneId") Long stadiumZoneId,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(#{matchId},#{stadiumZoneId},#{createdBy},#{zoneNameSnapshot},#{ticketPrice},'DRAFT',#{saleStartTime},#{saleEndTime})")
    @Options(useGeneratedKeys=true,keyProperty="matchZoneId") int insert(MatchTicketZone zone);
    @Update("UPDATE match_ticket_zone SET stadium_zone_id=#{stadiumZoneId},zone_name_snapshot=#{zoneNameSnapshot},ticket_price=#{ticketPrice},sale_start_time=#{saleStartTime},sale_end_time=#{saleEndTime},version=version+1 WHERE match_zone_id=#{matchZoneId}")
    int update(MatchTicketZone zone);
    @Update("UPDATE match_ticket_zone SET zone_status=#{status},version=version+1 WHERE match_zone_id=#{id}")
    int updateStatus(@Param("id") Long id,@Param("status") String status);
}
