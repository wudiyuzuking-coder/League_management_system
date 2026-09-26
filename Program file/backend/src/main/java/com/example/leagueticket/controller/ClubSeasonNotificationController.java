package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubSeasonNotificationService;
import com.example.leagueticket.vo.SeasonCancellationNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/club/season-notifications")
@Profile("dev")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB')")
public class ClubSeasonNotificationController {
    private final ClubSeasonNotificationService service;
    private final ClubDataScopeService scope;

    @GetMapping
    public Result<List<SeasonCancellationNotificationResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(service.listForClub(scope.requireBoundClubId(user)));
    }
}
