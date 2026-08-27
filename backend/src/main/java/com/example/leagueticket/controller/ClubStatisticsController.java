package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.StatisticsQueryRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.StatisticsService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/club/statistics") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB')")
public class ClubStatisticsController {
    private final StatisticsService service;
    @GetMapping("/overview") public Result<ClubStatisticsResponse> overview(@AuthenticationPrincipal AuthenticatedUser user,@Valid StatisticsQueryRequest q){return Result.success(service.clubOverview(user.clubId(),q));}
    @GetMapping("/matches") public Result<PageResponse<MatchStatisticsResponse>> matches(@AuthenticationPrincipal AuthenticatedUser user,@Valid StatisticsQueryRequest q){return Result.success(service.clubMatches(user.clubId(),q));}
}
