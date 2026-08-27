package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.PaymentRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.PaymentService;
import com.example.leagueticket.vo.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class PaymentController {
    private final PaymentService service;
    @PostMapping("/{orderId}/pay") public Result<PaymentResponse> pay(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long orderId,@Valid @RequestBody PaymentRequest request){return Result.success(service.pay(user.userId(),orderId,request));}
}
