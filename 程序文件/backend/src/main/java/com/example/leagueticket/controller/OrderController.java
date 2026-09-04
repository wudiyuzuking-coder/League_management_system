package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.OrderService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OrderController {
    private final OrderService service;
    @PostMapping public Result<OrderDetailResponse> create(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody OrderCreateRequest request){return Result.success(service.create(user.userId(),request));}
    @GetMapping public Result<PageResponse<OrderSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,@Valid OrderQueryRequest query){return Result.success(service.listOwned(user.userId(),query));}
    @GetMapping("/{id}") public Result<OrderDetailResponse> detail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return Result.success(service.detailOwned(user.userId(),id));}
    @PostMapping("/{id}/cancel") public Result<OrderDetailResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return Result.success(service.cancelOwned(user.userId(),id));}
}
