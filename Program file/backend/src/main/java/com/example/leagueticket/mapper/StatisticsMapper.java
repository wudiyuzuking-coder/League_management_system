package com.example.leagueticket.mapper;

import com.example.leagueticket.dto.StatisticsQueryRequest;
import com.example.leagueticket.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StatisticsMapper {
    OverviewStatisticsResponse overview(@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope);
    long countMatches(@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope);
    List<MatchStatisticsResponse> matchPage(@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope,@Param("offset")long offset,@Param("limit")int limit);
    MatchStatisticsResponse matchDetail(@Param("matchId")Long matchId,@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope);
    List<MatchZoneStatisticsResponse> matchZoneStatistics(@Param("matchId")Long matchId);
    List<ClubStatisticsResponse> clubStatistics(@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope);
    List<MatchStatisticsResponse> popularMatches(@Param("q")StatisticsQueryRequest query,@Param("clubScope")Long clubScope,@Param("limit")int limit);
    List<SalesTrendResponse> salesTrend(@Param("q")StatisticsQueryRequest query);
    RefundStatisticsResponse refundStatistics(@Param("q")StatisticsQueryRequest query);
    CheckinStatisticsResponse checkinStatistics(@Param("q")StatisticsQueryRequest query);
    @Select("SELECT #{seasonId} season_id,CAST(COALESCE(SUM(CASE WHEN oi.item_status='PAID' THEN oi.ticket_price ELSE 0 END),0) AS DECIMAL(14,2)) effective_revenue,CAST(COALESCE(SUM(CASE WHEN oi.item_status='PAID' THEN oi.ticket_price ELSE 0 END),0)*0.10 AS DECIMAL(14,2)) platform_share FROM match_info m LEFT JOIN ticket_order o ON o.match_id=m.match_id LEFT JOIN order_item oi ON oi.order_id=o.order_id WHERE m.season_id=#{seasonId}") SeasonRevenueResponse seasonRevenue(Long seasonId);
}
