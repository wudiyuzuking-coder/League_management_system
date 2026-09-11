package com.example.leagueticket.mapper;

import com.example.leagueticket.vo.ClubPerformanceResponse;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ClubDataMapper {
    String PERFORMANCE="""
      SELECT c.club_id,c.club_name,c.logo_url,
        COUNT(m.match_id) matches_played,
        COALESCE(SUM(CASE WHEN (m.home_club_id=c.club_id AND m.home_score>m.away_score) OR (m.away_club_id=c.club_id AND m.away_score>m.home_score) THEN 3 WHEN m.home_score=m.away_score THEN 1 ELSE 0 END),0) points,
        COALESCE(SUM(CASE WHEN (m.home_club_id=c.club_id AND m.home_score>m.away_score) OR (m.away_club_id=c.club_id AND m.away_score>m.home_score) THEN 1 ELSE 0 END),0) wins,
        COALESCE(SUM(CASE WHEN m.match_id IS NOT NULL AND m.home_score=m.away_score THEN 1 ELSE 0 END),0) draws,
        COALESCE(SUM(CASE WHEN (m.home_club_id=c.club_id AND m.home_score<m.away_score) OR (m.away_club_id=c.club_id AND m.away_score<m.home_score) THEN 1 ELSE 0 END),0) losses,
        COALESCE(SUM(CASE WHEN m.home_club_id=c.club_id THEN m.home_score ELSE m.away_score END),0) goals_for,
        COALESCE(SUM(CASE WHEN m.home_club_id=c.club_id THEN m.away_score ELSE m.home_score END),0) goals_against,
        COALESCE(SUM(CASE WHEN m.home_club_id=c.club_id THEN CAST(m.home_score AS SIGNED)-CAST(m.away_score AS SIGNED) ELSE CAST(m.away_score AS SIGNED)-CAST(m.home_score AS SIGNED) END),0) goal_difference
      FROM club_info c LEFT JOIN match_info m ON (m.home_club_id=c.club_id OR m.away_club_id=c.club_id)
        AND m.match_status='FINISHED' AND m.home_score IS NOT NULL AND m.away_score IS NOT NULL
      WHERE c.club_status='ACTIVE'
      """;
    @Select(PERFORMANCE+" AND c.club_id=#{clubId} GROUP BY c.club_id,c.club_name,c.logo_url") ClubPerformanceResponse performance(Long clubId);
    @Select(PERFORMANCE+" GROUP BY c.club_id,c.club_name,c.logo_url ORDER BY points DESC,goal_difference DESC,c.club_id") List<ClubPerformanceResponse> rankings();
    @Select("""
      SELECT CAST(COALESCE(SUM(oi.ticket_price * CASE WHEN m.home_club_id=#{clubId} THEN 0.60 ELSE 0.30 END),0) AS DECIMAL(14,2))
      FROM match_info m JOIN ticket_order o ON o.match_id=m.match_id JOIN order_item oi ON oi.order_id=o.order_id
      WHERE (m.home_club_id=#{clubId} OR m.away_club_id=#{clubId}) AND m.match_status='FINISHED' AND oi.item_status='PAID'
      """) BigDecimal revenue(Long clubId);
}
