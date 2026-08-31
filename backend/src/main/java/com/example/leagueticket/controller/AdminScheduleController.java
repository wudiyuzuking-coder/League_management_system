package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('EVENT_ADMIN')")
public class AdminScheduleController {
    private final SeasonScheduleService service;
    @PostMapping("/seasons/{seasonId}/schedule/generate") public Result<ScheduleDetailResponse> generate(@PathVariable Long seasonId){return Result.success(service.generateIfEligible(seasonId,"MANUAL"));}
    @GetMapping("/seasons/{seasonId}/schedule") public Result<ScheduleDetailResponse> detail(@PathVariable Long seasonId){return Result.success(service.get(seasonId));}
    @GetMapping("/schedules") public Result<PageResponse<SeasonScheduleBatch>> list(@Valid ScheduleQueryRequest query){return Result.success(service.list(query));}
    @PostMapping("/seasons/{seasonId}/schedule/confirm") public Result<ScheduleDetailResponse> confirm(@PathVariable Long seasonId,@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.confirm(seasonId,user.userId()));}
}
