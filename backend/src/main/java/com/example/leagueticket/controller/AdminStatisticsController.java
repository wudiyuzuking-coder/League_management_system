package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.service.StatisticsService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/statistics") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {
    private final StatisticsService service;
    @GetMapping("/overview") public Result<OverviewStatisticsResponse> overview(@Valid StatisticsQueryRequest q){return Result.success(service.overview(q));}
    @GetMapping("/matches") public Result<PageResponse<MatchStatisticsResponse>> matches(@Valid StatisticsQueryRequest q){return Result.success(service.matches(q));}
    @GetMapping("/matches/{id}") public Result<MatchStatisticsResponse> match(@PathVariable Long id){return Result.success(service.matchDetail(id));}
    @GetMapping("/clubs") public Result<List<ClubStatisticsResponse>> clubs(@Valid StatisticsQueryRequest q){return Result.success(service.clubs(q));}
    @GetMapping("/popular-matches") public Result<List<MatchStatisticsResponse>> popular(@Valid PopularMatchesQueryRequest q){return Result.success(service.popular(q));}
    @GetMapping("/sales-trend") public Result<List<SalesTrendResponse>> trend(@Valid StatisticsQueryRequest q){return Result.success(service.salesTrend(q));}
    @GetMapping("/refunds") public Result<RefundStatisticsResponse> refunds(@Valid StatisticsQueryRequest q){return Result.success(service.refunds(q));}
    @GetMapping("/checkins") public Result<CheckinStatisticsResponse> checkins(@Valid StatisticsQueryRequest q){return Result.success(service.checkins(q));}
}
