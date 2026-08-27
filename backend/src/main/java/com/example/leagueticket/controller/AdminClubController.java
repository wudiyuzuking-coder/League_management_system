package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.dto.ClubRequest;
import com.example.leagueticket.dto.ClubStatusRequest;
import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.dto.CoachStatusRequest;
import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.dto.PlayerSeasonStatRequest;
import com.example.leagueticket.dto.PlayerStatusRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.entity.CoachInfo;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.entity.PlayerSeasonStat;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.CoachInfoService;
import com.example.leagueticket.service.PlayerInfoService;
import com.example.leagueticket.service.PlayerSeasonStatService;
import com.example.leagueticket.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Profile("dev")
@RequiredArgsConstructor
public class AdminClubController {
    private final ClubInfoService clubService;
    private final PlayerInfoService playerService;
    private final CoachInfoService coachService;
    private final PlayerSeasonStatService statService;

    @GetMapping("/clubs")
    public Result<PageResponse<ClubInfo>> clubs(@Valid ClubQueryRequest request) {
        return Result.success(clubService.list(request));
    }

    @GetMapping("/clubs/{id}")
    public Result<ClubInfo> club(@PathVariable Long id) { return Result.success(clubService.getById(id)); }

    @PostMapping("/clubs")
    public Result<ClubInfo> createClub(@Valid @RequestBody ClubRequest request) { return Result.success(clubService.create(request)); }

    @PutMapping("/clubs/{id}")
    public Result<ClubInfo> updateClub(@PathVariable Long id, @Valid @RequestBody ClubRequest request) {
        return Result.success(clubService.update(id, request));
    }

    @PutMapping("/clubs/{id}/status")
    public Result<Void> updateClubStatus(@PathVariable Long id, @Valid @RequestBody ClubStatusRequest request) {
        clubService.updateStatus(id, request.clubStatus()); return Result.success();
    }

    @GetMapping("/clubs/{clubId}/players")
    public Result<List<PlayerInfo>> players(@PathVariable Long clubId) { return Result.success(playerService.listByClub(clubId)); }

    @PostMapping("/clubs/{clubId}/players")
    public Result<PlayerInfo> createPlayer(@PathVariable Long clubId, @Valid @RequestBody PlayerRequest request) {
        return Result.success(playerService.create(clubId, request));
    }

    @PutMapping("/players/{id}")
    public Result<PlayerInfo> updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return Result.success(playerService.update(playerService.getById(id).getClubId(), id, request));
    }

    @PutMapping("/players/{id}/status")
    public Result<Void> updatePlayerStatus(@PathVariable Long id, @Valid @RequestBody PlayerStatusRequest request) {
        playerService.updateStatus(playerService.getById(id).getClubId(), id, request.playerStatus()); return Result.success();
    }

    @GetMapping("/clubs/{clubId}/coaches")
    public Result<List<CoachInfo>> coaches(@PathVariable Long clubId) { return Result.success(coachService.listByClub(clubId)); }

    @PostMapping("/clubs/{clubId}/coaches")
    public Result<CoachInfo> createCoach(@PathVariable Long clubId, @Valid @RequestBody CoachRequest request) {
        return Result.success(coachService.create(clubId, request));
    }

    @PutMapping("/coaches/{id}")
    public Result<CoachInfo> updateCoach(@PathVariable Long id, @Valid @RequestBody CoachRequest request) {
        return Result.success(coachService.update(coachService.getById(id).getClubId(), id, request));
    }

    @PutMapping("/coaches/{id}/status")
    public Result<Void> updateCoachStatus(@PathVariable Long id, @Valid @RequestBody CoachStatusRequest request) {
        coachService.updateStatus(coachService.getById(id).getClubId(), id, request.coachStatus()); return Result.success();
    }

    @GetMapping("/clubs/{clubId}/player-season-stats")
    public Result<List<PlayerSeasonStat>> stats(@PathVariable Long clubId) { return Result.success(statService.listByClub(clubId)); }

    @PostMapping("/clubs/{clubId}/player-season-stats")
    public Result<PlayerSeasonStat> createStat(@PathVariable Long clubId, @Valid @RequestBody PlayerSeasonStatRequest request) {
        return Result.success(statService.create(clubId, request));
    }

    @PutMapping("/player-season-stats/{id}")
    public Result<PlayerSeasonStat> updateStat(@PathVariable Long id, @Valid @RequestBody PlayerSeasonStatRequest request) {
        return Result.success(statService.update(statService.getById(id).getClubId(), id, request));
    }
}
