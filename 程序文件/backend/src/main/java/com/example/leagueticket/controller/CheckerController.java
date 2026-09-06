package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.CheckinService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/checker") @Profile("dev") @RequiredArgsConstructor
public class CheckerController {
    private final CheckinService service;
    @GetMapping("/matches") @PreAuthorize("hasAnyRole('CHECKER','ADMIN')")
    public Result<List<CheckerMatchResponse>> matches(@AuthenticationPrincipal AuthenticatedUser user,@Valid CheckerMatchQueryRequest query){return Result.success(service.matches(user,query));}
    @PostMapping("/matches/{matchId}/checkin") @PreAuthorize("hasAnyRole('CHECKER','ADMIN')")
    public Result<CheckinResponse> checkin(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long matchId,@Valid @RequestBody CheckinRequest request){return Result.success(service.checkin(user,matchId,request));}
    @GetMapping("/checkins") @PreAuthorize("hasRole('CHECKER')")
    public Result<PageResponse<CheckinRecordResponse>> records(@AuthenticationPrincipal AuthenticatedUser user,@Valid CheckinQueryRequest query){return Result.success(service.ownRecords(user,query));}
}
