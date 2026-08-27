package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.CheckinQueryRequest;
import com.example.leagueticket.service.CheckinService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/checkins") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCheckinController {
    private final CheckinService service;
    @GetMapping public Result<PageResponse<CheckinRecordResponse>> list(@Valid CheckinQueryRequest query){return Result.success(service.adminRecords(query));}
    @GetMapping("/{id}") public Result<CheckinRecordResponse> detail(@PathVariable Long id){return Result.success(service.adminDetail(id));}
}
