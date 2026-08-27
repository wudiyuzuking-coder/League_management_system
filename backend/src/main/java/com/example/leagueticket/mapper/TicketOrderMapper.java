package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.TicketOrder;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketOrderMapper {
    String DETAIL_SELECT="""
        SELECT o.*,hc.club_name home_club_name,ac.club_name away_club_name,m.match_time,
          st.stadium_name,mz.zone_name_snapshot zone_name
        FROM ticket_order o JOIN match_info m ON m.match_id=o.match_id
        JOIN club_info hc ON hc.club_id=m.home_club_id JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        JOIN match_ticket_zone mz ON mz.match_zone_id=o.match_zone_id
        """;
    @Insert("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,cancel_reason) VALUES(#{orderNo},#{userId},#{matchId},#{matchZoneId},#{ticketCount},#{totalAmount},'PENDING_PAYMENT',#{expireTime},NULL)")
    @Options(useGeneratedKeys=true,keyProperty="orderId") int insert(TicketOrder order);
    @Select(DETAIL_SELECT+" WHERE o.order_id=#{id}") TicketOrder findDetail(Long id);
    @Select("SELECT * FROM ticket_order WHERE order_id=#{id} FOR UPDATE") TicketOrder findByIdForUpdate(Long id);
    @Select("""
        <script>SELECT COUNT(*) FROM ticket_order WHERE user_id=#{userId}
        <if test='status!=null and status!=""'>AND order_status=#{status}</if></script>
        """) long countOwned(@Param("userId")Long userId,@Param("status")String status);
    @Select("""
        <script>"""+DETAIL_SELECT+"""
        WHERE o.user_id=#{userId}<if test='status!=null and status!=""'> AND o.order_status=#{status}</if>
        ORDER BY o.created_at DESC,o.order_id DESC LIMIT #{limit} OFFSET #{offset}</script>
        """) List<TicketOrder> findOwnedPage(@Param("userId")Long userId,@Param("status")String status,@Param("offset")long offset,@Param("limit")int limit);
    @Update("UPDATE ticket_order SET order_status='CANCELLED',cancelled_at=#{now},cancel_reason=#{reason},version=version+1 WHERE order_id=#{id} AND order_status='PENDING_PAYMENT'")
    int cancelPending(@Param("id")Long id,@Param("reason")String reason,@Param("now")LocalDateTime now);
    @Update("UPDATE ticket_order SET order_status='PAID',paid_at=#{now},version=version+1 WHERE order_id=#{id} AND order_status='PENDING_PAYMENT'")
    int markPaid(@Param("id")Long id,@Param("now")LocalDateTime now);
    @Update("UPDATE ticket_order SET order_status='REFUND_PENDING',version=version+1 WHERE order_id=#{id} AND order_status='PAID'") int markRefundPending(Long id);
    @Update("UPDATE ticket_order SET order_status='REFUNDED',version=version+1 WHERE order_id=#{id} AND order_status='REFUND_PENDING'") int markRefunded(Long id);
    @Update("UPDATE ticket_order SET order_status='PAID',version=version+1 WHERE order_id=#{id} AND order_status='REFUND_PENDING'") int restorePaid(Long id);
    @Select("SELECT order_id FROM ticket_order WHERE order_status='PENDING_PAYMENT' AND expire_time<=#{now} ORDER BY expire_time,order_id LIMIT #{limit}")
    List<Long> findExpiredIds(@Param("now")LocalDateTime now,@Param("limit")int limit);
}
