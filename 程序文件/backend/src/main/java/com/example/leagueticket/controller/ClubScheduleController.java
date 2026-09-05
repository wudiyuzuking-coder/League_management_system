package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.ClubScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/club/schedules") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB')")
public class ClubScheduleController {
    private final SeasonScheduleService service;private final ClubDataScopeService scope;
    @GetMapping public Result<List<ClubScheduleResponse>> list(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.clubSchedules(scope.requireBoundClubId(user)));}
}
