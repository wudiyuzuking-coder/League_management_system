package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.RoundInfo;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.service.ClubSeasonRecordService;
import com.example.leagueticket.service.RoundInfoService;
import com.example.leagueticket.service.SeasonInfoService;
import com.example.leagueticket.service.SeasonLifecycleService;
import com.example.leagueticket.service.SeasonScheduleService;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.StandingResponse;
import com.example.leagueticket.vo.ScheduleDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController @RequestMapping("/api/admin") @Profile("dev") @RequiredArgsConstructor
public class AdminLeagueController {
    private final SeasonInfoService seasonService;
    private final SeasonLifecycleService lifecycleService;
    private final SeasonScheduleService scheduleService;
    private final RoundInfoService roundService;
    private final ClubSeasonRecordService recordService;
    @GetMapping("/seasons") public Result<List<SeasonInfo>> seasons(){return Result.success(seasonService.list());}
    @GetMapping("/seasons/{id}") public Result<SeasonInfo> season(@PathVariable Long id){return Result.success(seasonService.getById(id));}
    @PostMapping("/seasons") public Result<SeasonInfo> createSeason(@Valid @RequestBody SeasonRequest request){return Result.success(seasonService.create(request));}
    @PutMapping("/seasons/{id}") public Result<SeasonInfo> updateSeason(@PathVariable Long id,@Valid @RequestBody SeasonRequest request){return Result.success(seasonService.update(id,request));}
    @PutMapping("/seasons/{id}/status") public Result<SeasonInfo> updateSeasonStatus(@PathVariable Long id,@Valid @RequestBody SeasonStatusRequest request){return Result.success(lifecycleService.transitionCompatible(id,request.seasonStatus()));}
    @PostMapping("/seasons/{id}/registration/open") public Result<SeasonInfo> openRegistration(@PathVariable Long id){return Result.success(lifecycleService.openRegistration(id));}
    @PostMapping("/seasons/{id}/registration/close") public Result<ScheduleDetailResponse> closeRegistration(@PathVariable Long id,@AuthenticationPrincipal AuthenticatedUser user){return Result.success(scheduleService.closeRegistrationAndPublishSchedule(id,user.userId()));}
    @PostMapping("/seasons/{id}/finish") public Result<SeasonInfo> finishSeason(@PathVariable Long id){return Result.success(lifecycleService.finish(id));}
    @GetMapping("/seasons/{seasonId}/rounds") public Result<List<RoundInfo>> rounds(@PathVariable Long seasonId){return Result.success(roundService.listBySeason(seasonId));}
    @GetMapping("/seasons/{seasonId}/standings") public Result<List<StandingResponse>> standings(@PathVariable Long seasonId){return Result.success(recordService.standings(seasonId));}
    @PostMapping("/seasons/{seasonId}/rounds") public Result<RoundInfo> createRound(@PathVariable Long seasonId,@Valid @RequestBody RoundRequest request){return Result.success(roundService.create(seasonId,request));}
    @PutMapping("/rounds/{id}") public Result<RoundInfo> updateRound(@PathVariable Long id,@Valid @RequestBody RoundRequest request){return Result.success(roundService.update(id,request));}
    @PutMapping("/rounds/{id}/status") public Result<RoundInfo> updateRoundStatus(@PathVariable Long id,@Valid @RequestBody RoundStatusRequest request){return Result.success(roundService.updateStatus(id,request.roundStatus()));}
    @PostMapping("/seasons/{seasonId}/standings/init") public Result<Integer> initStandings(@PathVariable Long seasonId){return Result.success(recordService.initialize(seasonId));}
    @PutMapping("/season-records/{recordId}") public Result<StandingResponse> updateRecord(@PathVariable Long recordId,@Valid @RequestBody SeasonRecordRequest request){return Result.success(recordService.update(recordId,request));}
}
