package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubRequest;
import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.dto.CoachStatusRequest;
import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.dto.PlayerSeasonStatRequest;
import com.example.leagueticket.dto.PlayerStatusRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.entity.CoachInfo;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.entity.PlayerSeasonStat;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.CoachInfoService;
import com.example.leagueticket.service.PlayerInfoService;
import com.example.leagueticket.service.PlayerSeasonStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/club")
@Profile("dev")
@PreAuthorize("hasAuthority('CLUB_MANAGE_SELF')")
@RequiredArgsConstructor
public class ClubManagementController {
    private final ClubDataScopeService scopeService;
    private final ClubInfoService clubService;
    private final PlayerInfoService playerService;
    private final CoachInfoService coachService;
    private final PlayerSeasonStatService statService;

    @GetMapping("/profile")
    public Result<ClubInfo> profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(clubService.getById(scopeService.requireBoundClubId(user)));
    }

    @PutMapping("/profile")
    public Result<ClubInfo> updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody ClubRequest request) {
        return Result.success(clubService.update(scopeService.requireBoundClubId(user), request));
    }

    @GetMapping("/players")
    public Result<List<PlayerInfo>> players(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(playerService.listByClub(scopeService.requireBoundClubId(user)));
    }

    @PostMapping("/players")
    public Result<PlayerInfo> createPlayer(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody PlayerRequest request) {
        return Result.success(playerService.create(scopeService.requireBoundClubId(user), request));
    }

    @PutMapping("/players/{id}")
    public Result<PlayerInfo> updatePlayer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @Valid @RequestBody PlayerRequest request) {
        return Result.success(playerService.update(scopeService.requireBoundClubId(user), id, request));
    }

    @PutMapping("/players/{id}/status")
    public Result<Void> updatePlayerStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @Valid @RequestBody PlayerStatusRequest request) {
        playerService.updateStatus(scopeService.requireBoundClubId(user), id, request.playerStatus());
        return Result.success();
    }

    @GetMapping("/coaches")
    public Result<List<CoachInfo>> coaches(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(coachService.listByClub(scopeService.requireBoundClubId(user)));
    }

    @PostMapping("/coaches")
    public Result<CoachInfo> createCoach(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody CoachRequest request) {
        return Result.success(coachService.create(scopeService.requireBoundClubId(user), request));
    }

    @PutMapping("/coaches/{id}")
    public Result<CoachInfo> updateCoach(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                         @Valid @RequestBody CoachRequest request) {
        return Result.success(coachService.update(scopeService.requireBoundClubId(user), id, request));
    }

    @PutMapping("/coaches/{id}/status")
    public Result<Void> updateCoachStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                          @Valid @RequestBody CoachStatusRequest request) {
        coachService.updateStatus(scopeService.requireBoundClubId(user), id, request.coachStatus());
        return Result.success();
    }

    @GetMapping("/player-season-stats")
    public Result<List<PlayerSeasonStat>> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(statService.listByClub(scopeService.requireBoundClubId(user)));
    }

    @PostMapping("/player-season-stats")
    public Result<PlayerSeasonStat> createStat(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody PlayerSeasonStatRequest request) {
        return Result.success(statService.create(scopeService.requireBoundClubId(user), request));
    }

    @PutMapping("/player-season-stats/{id}")
    public Result<PlayerSeasonStat> updateStat(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                               @Valid @RequestBody PlayerSeasonStatRequest request) {
        return Result.success(statService.update(scopeService.requireBoundClubId(user), id, request));
    }
}
