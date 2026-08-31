package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.ETicket;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ETicketMapper {
    String DETAIL_SELECT="""
        SELECT t.*,o.match_id,hc.club_name home_club_name,ac.club_name away_club_name,m.match_time,
          st.stadium_name,oi.zone_name_snapshot zone_name,oi.row_no_snapshot row_no,
          oi.seat_no_snapshot seat_no,oi.ticket_price
        FROM e_ticket t JOIN ticket_order o ON o.order_id=t.order_id
        JOIN order_item oi ON oi.item_id=t.item_id AND oi.order_id=t.order_id
        JOIN match_info m ON m.match_id=o.match_id
        JOIN club_info hc ON hc.club_id=m.home_club_id JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        """;
    @Insert("INSERT INTO e_ticket(ticket_code,order_id,item_id,ticket_status,issued_at) VALUES(#{ticketCode},#{orderId},#{itemId},'UNUSED',#{issuedAt})")
    @Options(useGeneratedKeys=true,keyProperty="ticketId") int insert(ETicket ticket);
    @Select(DETAIL_SELECT+" WHERE t.order_id=#{orderId} ORDER BY t.ticket_id") List<ETicket> findByOrder(Long orderId);
    @Select(DETAIL_SELECT+" WHERE t.ticket_id=#{ticketId} AND o.user_id=#{userId}")
    ETicket findOwnedDetail(@Param("ticketId")Long ticketId,@Param("userId")Long userId);
    @Select(DETAIL_SELECT+" WHERE t.ticket_code=#{ticketCode}") ETicket findByCode(String ticketCode);
    @Select("SELECT * FROM e_ticket WHERE ticket_id=#{ticketId} FOR UPDATE") ETicket findByIdForUpdate(Long ticketId);
    @Select("SELECT * FROM e_ticket WHERE order_id=#{orderId} ORDER BY ticket_id FOR UPDATE") List<ETicket> findByOrderForUpdate(Long orderId);
    @Select("SELECT COUNT(*) FROM e_ticket WHERE order_id=#{orderId} AND ticket_status=#{status}") int countStatusByOrder(@Param("orderId")Long orderId,@Param("status")String status);
    @Update("UPDATE e_ticket SET ticket_status='REFUNDED',used_at=NULL WHERE order_id=#{orderId} AND ticket_status='UNUSED'") int markRefunded(Long orderId);
    @Update("UPDATE e_ticket SET ticket_status='USED',used_at=#{now} WHERE ticket_id=#{ticketId} AND ticket_status='UNUSED'") int markUsedForTest(@Param("ticketId")Long ticketId,@Param("now")java.time.LocalDateTime now);
    @Update("UPDATE e_ticket SET ticket_status='USED',used_at=#{now} WHERE ticket_id=#{ticketId} AND ticket_status='UNUSED'") int markUsed(@Param("ticketId")Long ticketId,@Param("now")java.time.LocalDateTime now);
    @Select("""
        <script>SELECT COUNT(*) FROM e_ticket t JOIN ticket_order o ON o.order_id=t.order_id
        WHERE o.user_id=#{userId}<if test='status!=null'> AND t.ticket_status=#{status}</if></script>
        """) long countOwned(@Param("userId")Long userId,@Param("status")String status);
    @Select("""
        <script>"""+DETAIL_SELECT+"""
        WHERE o.user_id=#{userId}<if test='status!=null'> AND t.ticket_status=#{status}</if>
        ORDER BY t.issued_at DESC,t.ticket_id DESC LIMIT #{limit} OFFSET #{offset}</script>
        """) List<ETicket> findOwnedPage(@Param("userId")Long userId,@Param("status")String status,
        @Param("offset")long offset,@Param("limit")int limit);
}
