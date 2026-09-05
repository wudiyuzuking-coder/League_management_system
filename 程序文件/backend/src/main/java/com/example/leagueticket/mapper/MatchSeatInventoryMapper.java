package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.MatchSeatInventory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MatchSeatInventoryMapper {
    String LAYOUT_COLUMNS="i.inventory_id,i.match_id,i.match_zone_id,i.stadium_seat_id,i.inventory_status,i.lock_order_id,i.locked_at,i.lock_expire_time,i.version,s.row_seq row_no,s.row_no row_label,s.seat_seq seat_no,s.seat_no seat_label";
    @Select("SELECT "+LAYOUT_COLUMNS+" FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.match_zone_id=#{zoneId} ORDER BY s.row_seq,s.seat_seq")
    List<MatchSeatInventory> findLayout(Long zoneId);
    @Select("SELECT "+LAYOUT_COLUMNS+" FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.inventory_id=#{id}")
    MatchSeatInventory findById(Long id);
    @Select("""
        SELECT i.inventory_id,i.match_id,i.match_zone_id,i.stadium_seat_id,i.inventory_status,i.version,
          s.row_seq row_no,s.row_no row_label,s.seat_seq seat_no,s.seat_no seat_label,
          bounds.min_seat_no physical_min_seat_no,bounds.max_seat_no physical_max_seat_no
        FROM match_seat_inventory i
        JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id
        JOIN match_ticket_zone mz ON mz.match_zone_id=i.match_zone_id
        JOIN (SELECT stadium_zone_id,row_seq,MIN(seat_seq) min_seat_no,MAX(seat_seq) max_seat_no
              FROM stadium_seat GROUP BY stadium_zone_id,row_seq) bounds
          ON bounds.stadium_zone_id=mz.stadium_zone_id AND bounds.row_seq=s.row_seq
        WHERE i.match_zone_id=#{zoneId}
        ORDER BY s.row_seq,s.seat_seq
        """) List<MatchSeatInventory> findForAllocation(Long zoneId);
    @Select("""
        SELECT i.inventory_id,i.match_id,i.match_zone_id,i.stadium_seat_id,i.inventory_status,i.version,
          s.row_seq row_no,s.row_no row_label,s.seat_seq seat_no,s.seat_no seat_label,
          bounds.min_seat_no physical_min_seat_no,bounds.max_seat_no physical_max_seat_no
        FROM match_seat_inventory i
        JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id
        JOIN match_ticket_zone mz ON mz.match_zone_id=i.match_zone_id
        JOIN (SELECT stadium_zone_id,row_seq,MIN(seat_seq) min_seat_no,MAX(seat_seq) max_seat_no
              FROM stadium_seat GROUP BY stadium_zone_id,row_seq) bounds
          ON bounds.stadium_zone_id=mz.stadium_zone_id AND bounds.row_seq=s.row_seq
        WHERE i.match_zone_id=#{zoneId}
        ORDER BY i.inventory_id FOR UPDATE
        """) List<MatchSeatInventory> findForAllocationForUpdate(Long zoneId);
    @Select("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=#{zoneId}") long countTotal(Long zoneId);
    @Select("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=#{zoneId} AND inventory_status=#{status}") long countStatus(@Param("zoneId") Long zoneId,@Param("status") String status);
    @Select("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=#{zoneId} AND inventory_status IN ('LOCKED','SOLD')") long countUsed(Long zoneId);
    @Insert("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status,lock_order_id,locked_at,lock_expire_time,version) SELECT #{matchId},#{matchZoneId},s.stadium_seat_id,'AVAILABLE',NULL,NULL,NULL,0 FROM stadium_seat s WHERE s.stadium_zone_id=#{stadiumZoneId} AND s.seat_status='ACTIVE'")
    int generate(@Param("matchId") Long matchId,@Param("matchZoneId") Long matchZoneId,@Param("stadiumZoneId") Long stadiumZoneId);
    @Update("UPDATE match_seat_inventory SET inventory_status=#{status},version=version+1 WHERE inventory_id=#{id}")
    int updateStatus(@Param("id") Long id,@Param("status") String status);
    @Update("UPDATE match_seat_inventory SET inventory_status='DISABLED',version=version+1 WHERE inventory_id=#{id} AND inventory_status='AVAILABLE' AND version=#{version}")
    int claimAvailableForTest(@Param("id") Long id,@Param("version") Integer version);
    @Update("UPDATE match_seat_inventory SET inventory_status='LOCKED',lock_order_id=#{orderId},locked_at=#{lockedAt},lock_expire_time=#{expireTime},version=version+1 WHERE inventory_id=#{id} AND inventory_status='AVAILABLE' AND version=#{version}")
    int lockAvailable(@Param("id")Long id,@Param("version")Integer version,@Param("orderId")Long orderId,
                      @Param("lockedAt")java.time.LocalDateTime lockedAt,@Param("expireTime")java.time.LocalDateTime expireTime);
    @Update("UPDATE match_seat_inventory SET inventory_status='AVAILABLE',lock_order_id=NULL,locked_at=NULL,lock_expire_time=NULL,version=version+1 WHERE lock_order_id=#{orderId} AND inventory_status='LOCKED'")
    int releaseLockedByOrder(Long orderId);
    @Select("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=#{orderId} AND inventory_status='LOCKED'") int countLockedByOrder(Long orderId);
    @Select("""
        SELECT COUNT(*) FROM order_item oi JOIN match_seat_inventory i ON i.inventory_id=oi.inventory_id
        WHERE oi.order_id=#{orderId} AND oi.item_status='LOCKED' AND i.inventory_status='LOCKED'
          AND i.lock_order_id=#{orderId}
        """) int countPayableByOrder(Long orderId);
    @Update("UPDATE match_seat_inventory SET inventory_status='SOLD',lock_order_id=NULL,locked_at=NULL,lock_expire_time=NULL,version=version+1 WHERE lock_order_id=#{orderId} AND inventory_status='LOCKED'")
    int markSoldByOrder(Long orderId);
    @Select("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=#{orderId} AND i.inventory_status='SOLD'") int countSoldByOrder(Long orderId);
    @Update("UPDATE match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id SET i.inventory_status='AVAILABLE',i.lock_order_id=NULL,i.locked_at=NULL,i.lock_expire_time=NULL,i.version=i.version+1 WHERE oi.order_id=#{orderId} AND i.inventory_status='SOLD'") int releaseSoldByOrder(Long orderId);
}
