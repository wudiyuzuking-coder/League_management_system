package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.entity.RoundInfo;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.service.ClubSeasonRecordService;
import com.example.leagueticket.service.RoundInfoService;
import com.example.leagueticket.service.SeasonInfoService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.service.PublicSeasonVisibilityService;
import com.example.leagueticket.vo.StandingResponse;
import com.example.leagueticket.vo.UserSeasonScheduleResponse;
import com.example.leagueticket.vo.PublicSeasonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Profile("dev") @RequiredArgsConstructor
public class LeagueController {
    private final SeasonInfoService seasonService;
    private final RoundInfoService roundService;
    private final ClubSeasonRecordService recordService;
    private final SeasonScheduleService scheduleService;
    private final PublicSeasonVisibilityService publicVisibility;
    @GetMapping("/api/seasons") public Result<List<PublicSeasonResponse>> seasons(){return Result.success(seasonService.listPublic());}
    @GetMapping("/api/seasons/{id}") public Result<PublicSeasonResponse> season(@PathVariable Long id){return Result.success(seasonService.getPublicById(id));}
    @GetMapping("/api/seasons/{seasonId}/rounds") public Result<List<RoundInfo>> rounds(@PathVariable Long seasonId){publicVisibility.requirePublicVisibleSeason(seasonId);return Result.success(roundService.listBySeason(seasonId));}
    @GetMapping("/api/rounds/{id}") public Result<RoundInfo> round(@PathVariable Long id){return Result.success(roundService.getPublicById(id));}
    @GetMapping("/api/seasons/{seasonId}/standings") public Result<List<StandingResponse>> standings(@PathVariable Long seasonId){publicVisibility.requirePublicVisibleSeason(seasonId);return Result.success(recordService.standings(seasonId));}
    @GetMapping("/api/seasons/{seasonId}/schedule") public Result<UserSeasonScheduleResponse> schedule(@PathVariable Long seasonId){return Result.success(scheduleService.getPublicConfirmed(seasonId));}
}
