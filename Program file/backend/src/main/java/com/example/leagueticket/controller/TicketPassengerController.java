package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.PrefilledPassengerRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.TicketPassengerService;
import com.example.leagueticket.vo.PrefilledPassengerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Profile("dev") @RequiredArgsConstructor @RequestMapping("/api/user/prefilled-passengers") @PreAuthorize("hasRole('USER')")
public class TicketPassengerController {
    private final TicketPassengerService service;
    @GetMapping public Result<List<PrefilledPassengerResponse>> list(@AuthenticationPrincipal AuthenticatedUser user){return Result.success(service.list(user.userId()));}
    @PostMapping public Result<PrefilledPassengerResponse> add(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody PrefilledPassengerRequest request){return Result.success(service.add(user.userId(),request));}
    @DeleteMapping("/{id}") public Result<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){service.delete(user.userId(),id);return Result.success();}
}
