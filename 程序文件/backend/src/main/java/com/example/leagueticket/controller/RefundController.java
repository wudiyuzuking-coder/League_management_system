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

@RestController @RequestMapping("/api") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class RefundController {
    private final RefundService service;
    @PostMapping("/orders/{orderId}/refund") public Result<RefundResponse> apply(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long orderId,@Valid @RequestBody RefundApplyRequest request){return Result.success(service.apply(user.userId(),orderId,request));}
    @GetMapping("/refunds") public Result<PageResponse<RefundResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,@Valid RefundQueryRequest query){return Result.success(service.listOwned(user.userId(),query));}
    @GetMapping("/refunds/{id}") public Result<RefundResponse> detail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return Result.success(service.detailOwned(user.userId(),id));}
}
