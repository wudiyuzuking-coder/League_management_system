package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.EnrollmentRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubSeasonEnrollmentService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/club/enrollments") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB')")
public class ClubEnrollmentController {
    private final ClubSeasonEnrollmentService service;
    private final ClubDataScopeService scope;
    @GetMapping("/available-seasons") public Result<List<AvailableSeasonResponse>> available(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.availableSeasons(scope.requireBoundClubId(user)));}
    @PostMapping public Result<EnrollmentResponse> submit(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody EnrollmentRequest request){return Result.success(service.submit(scope.requireBoundClubId(user),request));}
    @GetMapping public Result<List<EnrollmentResponse>> list(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.listClub(scope.requireBoundClubId(user)));}
    @GetMapping("/{id}") public Result<EnrollmentResponse> detail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return Result.success(service.detailClub(scope.requireBoundClubId(user),id));}
}
