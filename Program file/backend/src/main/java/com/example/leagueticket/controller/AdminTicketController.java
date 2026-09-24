package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin") @Profile("dev") @RequiredArgsConstructor
public class AdminTicketController {
    private final MatchTicketZoneService zoneService;
    private final MatchSeatInventoryService inventoryService;
    private final SeatAllocateService seatAllocateService;

    @GetMapping("/matches/{matchId}/ticket-zones") public Result<List<MatchTicketZoneResponse>> list(@PathVariable Long matchId){return Result.success(zoneService.list(matchId));}
    @GetMapping("/match-ticket-zones/{id}") public Result<MatchTicketZoneResponse> detail(@PathVariable Long id){return Result.success(zoneService.detail(id));}
    @GetMapping("/match-ticket-zones/{id}/inventory") public Result<List<InventoryRowResponse>> inventory(@PathVariable Long id){return Result.success(inventoryService.layout(id));}
    @PostMapping("/match-ticket-zones/{id}/seat-allocation/debug") public Result<SeatAllocationDebugResponse> debug(@PathVariable Long id,@Valid @RequestBody SeatAllocationRequest request){return Result.success(seatAllocateService.debug(id,request.ticketCount()));}
}
