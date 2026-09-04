package com.example.leagueticket.mapper;

import com.example.leagueticket.dto.StatisticsQueryRequest;
import com.example.leagueticket.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
}
