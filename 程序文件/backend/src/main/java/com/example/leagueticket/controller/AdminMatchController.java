package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.service.MatchInfoService;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.MatchResultReminderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/matches") @Profile("dev") @RequiredArgsConstructor
public class AdminMatchController {
    private final MatchInfoService service;
    @GetMapping public Result<PageResponse<MatchInfo>> list(@Valid MatchQueryRequest query){return Result.success(service.list(query));}
    @GetMapping("/result-reminders") public Result<PageResponse<MatchResultReminderResponse>> resultReminders(@Valid MatchResultReminderQueryRequest query){return Result.success(service.resultReminders(query));}
    @GetMapping("/{id}") public Result<MatchInfo> detail(@PathVariable Long id){return Result.success(service.getById(id));}
    @PostMapping public Result<MatchInfo> create(@Valid @RequestBody MatchRequest request){return Result.success(service.create(request));}
    @PutMapping("/{id}") public Result<MatchInfo> update(@PathVariable Long id,@Valid @RequestBody MatchRequest request){return Result.success(service.update(id,request));}
    @PutMapping("/{id}/status") public Result<MatchInfo> status(@PathVariable Long id,@Valid @RequestBody MatchStatusRequest request){return Result.success(service.updateStatus(id,request.matchStatus()));}
    @PutMapping("/{id}/score") public Result<MatchInfo> score(@PathVariable Long id,@Valid @RequestBody MatchScoreRequest request){return Result.success(service.updateScore(id,request));}
}
