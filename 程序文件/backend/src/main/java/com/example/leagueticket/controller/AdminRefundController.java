package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.RefundService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/refunds") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('EVENT_ADMIN')")
public class AdminRefundController {
    private final RefundService service;
    @GetMapping public Result<PageResponse<RefundResponse>> list(@Valid RefundQueryRequest query){return Result.success(service.listAdmin(query));}
    @GetMapping("/{id}") public Result<RefundResponse> detail(@PathVariable Long id){return Result.success(service.detailAdmin(id));}
    @PostMapping("/{id}/approve") public Result<RefundResponse> approve(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@Valid @RequestBody RefundAuditRequest request){return Result.success(service.approve(user.userId(),id,request));}
    @PostMapping("/{id}/reject") public Result<RefundResponse> reject(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@Valid @RequestBody RefundAuditRequest request){return Result.success(service.reject(user.userId(),id,request));}
}
