package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.CheckinRecord;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CheckinRecordMapper {
    String DETAIL_SELECT = """
        SELECT c.*,t.ticket_status,t.used_at,u.username checker_username,u.display_name checker_name,
          hc.club_name home_club_name,ac.club_name away_club_name,m.match_time,st.stadium_name,
          oi.zone_name_snapshot zone_name,oi.row_no_snapshot row_no,oi.seat_no_snapshot seat_no
        FROM checkin_record c
        JOIN match_info m ON m.match_id=c.match_id
        JOIN club_info hc ON hc.club_id=m.home_club_id
        JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        JOIN sys_user u ON u.user_id=c.checker_id
        LEFT JOIN e_ticket t ON t.ticket_id=c.ticket_id
        LEFT JOIN order_item oi ON oi.item_id=t.item_id AND oi.order_id=t.order_id
        """;

    @Insert("INSERT INTO checkin_record(match_id,ticket_id,scanned_ticket_code,checker_id,check_result,check_time,remark) VALUES(#{matchId},#{ticketId},#{scannedTicketCode},#{checkerId},#{checkResult},#{checkTime},#{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "checkinId")
    int insert(CheckinRecord record);

    @Select(DETAIL_SELECT + " WHERE c.checkin_id=#{id}")
    CheckinRecord findDetail(Long id);

    @Select("""
        <script>SELECT COUNT(*) FROM checkin_record c WHERE 1=1
        <if test='checkerId!=null'>AND c.checker_id=#{checkerId}</if>
        <if test='matchId!=null'>AND c.match_id=#{matchId}</if>
        <if test='result!=null'>AND c.check_result=#{result}</if>
        <if test='startTime!=null'>AND c.check_time&gt;=#{startTime}</if>
        <if test='endTime!=null'>AND c.check_time&lt;=#{endTime}</if>
        </script>
        """)
    long count(@Param("checkerId") Long checkerId, @Param("matchId") Long matchId,
            @Param("result") String result, @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("""
        <script>""" + DETAIL_SELECT + """
        WHERE 1=1
        <if test='checkerId!=null'>AND c.checker_id=#{checkerId}</if>
        <if test='matchId!=null'>AND c.match_id=#{matchId}</if>
        <if test='result!=null'>AND c.check_result=#{result}</if>
        <if test='startTime!=null'>AND c.check_time&gt;=#{startTime}</if>
        <if test='endTime!=null'>AND c.check_time&lt;=#{endTime}</if>
        ORDER BY c.check_time DESC,c.checkin_id DESC LIMIT #{limit} OFFSET #{offset}</script>
        """)
    List<CheckinRecord> findPage(@Param("checkerId") Long checkerId,
            @Param("matchId") Long matchId, @Param("result") String result,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
            @Param("offset") long offset, @Param("limit") int limit);
}
