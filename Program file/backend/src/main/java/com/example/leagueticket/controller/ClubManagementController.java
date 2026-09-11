package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubProfileRequest;
import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.dto.CoachStatusRequest;
import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.dto.PlayerSeasonStatRequest;
import com.example.leagueticket.dto.PlayerStatusRequest;
import com.example.leagueticket.dto.PlayerAdjustRequest;
import com.example.leagueticket.dto.PlayerReturnRequest;
import com.example.leagueticket.dto.CoachAdjustRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.entity.CoachInfo;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.entity.PlayerSeasonStat;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubHomeStadiumService;
import com.example.leagueticket.vo.ClubProfileResponse;
import com.example.leagueticket.service.CoachInfoService;
import com.example.leagueticket.service.PlayerInfoService;
import com.example.leagueticket.service.PlayerSeasonStatService;
import com.example.leagueticket.service.ClubPersonnelService;
import com.example.leagueticket.service.ClubLogoService;
import com.example.leagueticket.vo.PersonnelOverviewResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/club")
@Profile("dev")
@PreAuthorize("hasAuthority('CLUB_MANAGE_SELF')")
@RequiredArgsConstructor
public class ClubManagementController {
    private final ClubDataScopeService scopeService;
    private final ClubHomeStadiumService homeStadiumService;
    private final PlayerInfoService playerService;
    private final CoachInfoService coachService;
    private final PlayerSeasonStatService statService;
    private final ClubPersonnelService personnelService;
    private final ClubLogoService logoService;

    @PostMapping("/profile/logo") public Result<com.example.leagueticket.vo.AvatarResponse> uploadLogo(@AuthenticationPrincipal AuthenticatedUser user,@RequestParam("file") MultipartFile file){return Result.success(logoService.upload(scopeService.requireBoundClubId(user),file));}

    @GetMapping("/personnel/overview") public Result<PersonnelOverviewResponse> personnel(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(personnelService.overview(scopeService.requireBoundClubId(user)));}

    @GetMapping("/profile")
    public Result<ClubProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(homeStadiumService.profile(scopeService.requireBoundClubId(user)));
    }

    @PutMapping("/profile")
    public Result<ClubProfileResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody ClubProfileRequest request) {
        return Result.success(homeStadiumService.updateProfile(scopeService.requireBoundClubId(user), request));
    }

    @GetMapping("/players")
    public Result<List<PlayerInfo>> players(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(playerService.listByClub(scopeService.requireBoundClubId(user)));
    }

    @PostMapping("/players")
    public Result<PlayerInfo> createPlayer(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody PlayerRequest request) {
        if(request.birthYear()==null||request.nationality()==null||request.nationality().isBlank()||request.lineupRole()==null||request.lineupRole().isBlank())
            throw new com.example.leagueticket.exception.BusinessException("球员姓名、出生年份、国籍、首发/替补均为必填");
        return Result.success(playerService.create(scopeService.requireBoundClubId(user), request));
    }

    @PutMapping("/players/{id}")
    public Result<PlayerInfo> updatePlayer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @Valid @RequestBody PlayerAdjustRequest request) {
        return Result.success(playerService.adjust(scopeService.requireBoundClubId(user), id, request));
    }

    @PutMapping("/players/{id}/return") public Result<PlayerInfo> returnPlayer(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@Valid @RequestBody PlayerReturnRequest request){return Result.success(playerService.returnToTeam(scopeService.requireBoundClubId(user),id,request));}
    @DeleteMapping("/players/{id}") public Result<Void> cleanupPlayer(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){playerService.cleanup(scopeService.requireBoundClubId(user),id);return Result.success();}

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
        if(request.birthYear()==null||request.nationality()==null||request.nationality().isBlank())
            throw new com.example.leagueticket.exception.BusinessException("教练姓名、国籍和出生年份均为必填");
        return Result.success(coachService.create(scopeService.requireBoundClubId(user), request));
    }

    @PutMapping("/coaches/{id}")
    public Result<CoachInfo> updateCoach(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                         @Valid @RequestBody CoachAdjustRequest request) {
        return Result.success(coachService.adjust(scopeService.requireBoundClubId(user), id, request));
    }

    @DeleteMapping("/coaches/{id}") public Result<Void> cleanupCoach(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){coachService.cleanup(scopeService.requireBoundClubId(user),id);return Result.success();}

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
