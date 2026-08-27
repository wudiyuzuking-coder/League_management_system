package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.TicketQueryRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.ETicketService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tickets") @Profile("dev") @RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ETicketController {
    private final ETicketService service;
    @GetMapping public Result<PageResponse<ETicketResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,@Valid TicketQueryRequest query){return Result.success(service.listOwned(user.userId(),query));}
    @GetMapping("/{id}") public Result<ETicketResponse> detail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return Result.success(service.detailOwned(user.userId(),id));}
}
