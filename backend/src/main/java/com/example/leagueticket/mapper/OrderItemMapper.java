package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.OrderItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderItemMapper {
    @Insert("INSERT INTO order_item(order_id,inventory_id,ticket_price,zone_name_snapshot,row_no_snapshot,seat_no_snapshot,item_status) VALUES(#{orderId},#{inventoryId},#{ticketPrice},#{zoneNameSnapshot},#{rowNoSnapshot},#{seatNoSnapshot},'LOCKED')")
    @Options(useGeneratedKeys=true,keyProperty="itemId") int insert(OrderItem item);
    @Select("SELECT * FROM order_item WHERE order_id=#{orderId} ORDER BY item_id") List<OrderItem> findByOrder(Long orderId);
    @Update("UPDATE order_item SET item_status='CANCELLED' WHERE order_id=#{orderId} AND item_status='LOCKED'") int cancelLocked(Long orderId);
    @Update("UPDATE order_item SET item_status='PAID' WHERE order_id=#{orderId} AND item_status='LOCKED'") int markPaid(Long orderId);
    @Select("SELECT COUNT(*) FROM order_item WHERE order_id=#{orderId} AND item_status='LOCKED'") int countLockedByOrder(Long orderId);
    @Select("SELECT COUNT(*) FROM order_item WHERE order_id=#{orderId}") int countByOrder(Long orderId);
    @Select("SELECT COUNT(*) FROM order_item WHERE order_id=#{orderId} AND item_status=#{status}") int countStatusByOrder(@Param("orderId")Long orderId,@Param("status")String status);
    @Update("UPDATE order_item SET item_status='REFUNDED' WHERE order_id=#{orderId} AND item_status='PAID'") int markRefunded(Long orderId);
}
