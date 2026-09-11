package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.RefundApply;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RefundApplyMapper {
    String DETAIL_SELECT="""
        SELECT r.*,o.order_no,o.match_id,u.username,hc.club_name home_club_name,
          ac.club_name away_club_name,m.match_time,st.stadium_name,mz.zone_name_snapshot zone_name
        FROM refund_apply r JOIN ticket_order o ON o.order_id=r.order_id
        JOIN sys_user u ON u.user_id=r.applicant_id JOIN match_info m ON m.match_id=o.match_id
        JOIN club_info hc ON hc.club_id=m.home_club_id JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        JOIN match_ticket_zone mz ON mz.match_zone_id=o.match_zone_id
        """;
    @Insert("INSERT INTO refund_apply(refund_no,order_id,applicant_id,reason,refund_amount,refund_rate,fee_amount,refund_status,processing_mode,audit_time,created_at) VALUES(#{refundNo},#{orderId},#{applicantId},#{reason},#{refundAmount},#{refundRate},#{feeAmount},'APPROVED','AUTO',#{auditTime},#{createdAt})")
    @Options(useGeneratedKeys=true,keyProperty="refundId") int insert(RefundApply refund);
    @Select("SELECT * FROM refund_apply WHERE refund_id=#{id} FOR UPDATE") RefundApply findByIdForUpdate(Long id);
    @Select("SELECT COUNT(*) FROM refund_apply WHERE order_id=#{orderId}") int countByOrder(Long orderId);
    @Select(DETAIL_SELECT+" WHERE r.refund_id=#{id}") RefundApply findDetail(Long id);
    @Select(DETAIL_SELECT+" WHERE r.refund_id=#{id} AND r.applicant_id=#{userId}") RefundApply findOwnedDetail(@Param("id")Long id,@Param("userId")Long userId);
    @Select(DETAIL_SELECT+" WHERE r.order_id=#{orderId} LIMIT 1") RefundApply findByOrder(Long orderId);
    @Select("""
        <script>SELECT COUNT(*) FROM refund_apply r JOIN ticket_order o ON o.order_id=r.order_id JOIN sys_user u ON u.user_id=r.applicant_id
        WHERE r.applicant_id=#{userId}<if test='status!=null'> AND r.refund_status=#{status}</if></script>
        """) long countOwned(@Param("userId")Long userId,@Param("status")String status);
    @Select("<script>"+DETAIL_SELECT+" WHERE r.applicant_id=#{userId} " +
        "<if test='status!=null'> AND r.refund_status=#{status}</if> " +
        "ORDER BY r.created_at DESC,r.refund_id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<RefundApply> findOwnedPage(@Param("userId")Long userId,@Param("status")String status,@Param("offset")long offset,@Param("limit")int limit);
}
