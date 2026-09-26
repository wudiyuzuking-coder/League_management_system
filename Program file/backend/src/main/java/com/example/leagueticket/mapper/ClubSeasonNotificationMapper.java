package com.example.leagueticket.mapper;

import com.example.leagueticket.vo.SeasonCancellationNotificationResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ClubSeasonNotificationMapper {
    @Insert("""
        INSERT INTO club_season_notification
          (season_id,club_id,notification_type,message,occurred_at)
        SELECT e.season_id,e.club_id,'SEASON_CANCELLED',#{message},#{occurredAt}
        FROM club_season_enrollment e
        WHERE e.season_id=#{seasonId} AND e.enrollment_status='SUBMITTED'
        ON DUPLICATE KEY UPDATE notification_id=notification_id
        """)
    int insertCancellationNotifications(@Param("seasonId") Long seasonId,
                                        @Param("message") String message,
                                        @Param("occurredAt") LocalDateTime occurredAt);

    @Select("""
        SELECT n.notification_id,n.season_id,s.season_name,s.season_status,
          n.notification_type,n.message,s.cancel_reason,s.cancelled_at,n.occurred_at
        FROM club_season_notification n
        JOIN season_info s ON s.season_id=n.season_id
        WHERE n.club_id=#{clubId}
        ORDER BY n.occurred_at DESC,n.notification_id DESC
        """)
    List<SeasonCancellationNotificationResponse> findByClub(Long clubId);
}
