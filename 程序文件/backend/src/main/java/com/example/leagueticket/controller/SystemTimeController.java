package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.SystemTimeUpdateRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.SystemTimeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-time")
@Profile("dev")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','CLUB','EVENT_ADMIN','ADMIN')")
public class SystemTimeController {
    private final SystemTimeService service;
    @GetMapping public Result<SystemTimeResponse> get(){return Result.success(service.getTime());}
    @PutMapping public Result<SystemTimeResponse> set(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody SystemTimeUpdateRequest request){return Result.success(service.setCurrentSystemTime(request.targetTime(),user));}
    @PostMapping("/reset") public Result<SystemTimeResponse> reset(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.resetToRealTime(user));}
}
