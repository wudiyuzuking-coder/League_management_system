package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.vo.*;
import java.util.List;

public interface StatisticsService {
    OverviewStatisticsResponse overview(StatisticsQueryRequest query);
    PageResponse<MatchStatisticsResponse> matches(StatisticsQueryRequest query);
    MatchStatisticsResponse matchDetail(Long matchId);
    List<ClubStatisticsResponse> clubs(StatisticsQueryRequest query);
    List<MatchStatisticsResponse> popular(PopularMatchesQueryRequest query);
    List<SalesTrendResponse> salesTrend(StatisticsQueryRequest query);
    RefundStatisticsResponse refunds(StatisticsQueryRequest query);
    CheckinStatisticsResponse checkins(StatisticsQueryRequest query);
    ClubStatisticsResponse clubOverview(Long clubId,StatisticsQueryRequest query);
    PageResponse<MatchStatisticsResponse> clubMatches(Long clubId,StatisticsQueryRequest query);
}
