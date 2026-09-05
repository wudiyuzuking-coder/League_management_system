package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.EnrollmentQueryRequest;
import com.example.leagueticket.service.ClubSeasonEnrollmentService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/enrollments") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('EVENT_ADMIN')")
public class AdminEnrollmentController {
    private final ClubSeasonEnrollmentService service;
    @GetMapping public Result<PageResponse<EnrollmentResponse>> list(@Valid EnrollmentQueryRequest query){return Result.success(service.listAdmin(query));}
    @GetMapping("/{id}") public Result<EnrollmentResponse> detail(@PathVariable Long id){return Result.success(service.detailAdmin(id));}
}
