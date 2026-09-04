package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin") @Profile("dev") @RequiredArgsConstructor
public class AdminTicketController {
    private final MatchTicketZoneService zoneService;
    private final MatchSeatInventoryService inventoryService;
    private final SeatAllocateService seatAllocateService;

    @GetMapping("/matches/{matchId}/ticket-zones") public Result<List<MatchTicketZoneResponse>> list(@PathVariable Long matchId){return Result.success(zoneService.list(matchId));}
    @PostMapping("/matches/{matchId}/ticket-zones") public Result<MatchTicketZone> create(@PathVariable Long matchId,@AuthenticationPrincipal AuthenticatedUser principal,@Valid @RequestBody MatchTicketZoneRequest request){return Result.success(zoneService.create(matchId,principal.userId(),request));}
    @GetMapping("/match-ticket-zones/{id}") public Result<MatchTicketZoneResponse> detail(@PathVariable Long id){return Result.success(zoneService.detail(id));}
    @PutMapping("/match-ticket-zones/{id}") public Result<MatchTicketZone> update(@PathVariable Long id,@Valid @RequestBody MatchTicketZoneRequest request){return Result.success(zoneService.update(id,request));}
    @PutMapping("/match-ticket-zones/{id}/status") public Result<MatchTicketZone> status(@PathVariable Long id,@Valid @RequestBody MatchTicketZoneStatusRequest request){return Result.success(zoneService.updateStatus(id,request.zoneStatus()));}
    @PostMapping("/match-ticket-zones/{id}/inventory/generate") public Result<Integer> generate(@PathVariable Long id){return Result.success(inventoryService.generate(id));}
    @GetMapping("/match-ticket-zones/{id}/inventory") public Result<List<InventoryRowResponse>> inventory(@PathVariable Long id){return Result.success(inventoryService.layout(id));}
    @PutMapping("/match-seat-inventory/{id}/status") public Result<MatchSeatInventory> inventoryStatus(@PathVariable Long id,@Valid @RequestBody MatchInventoryStatusRequest request){return Result.success(inventoryService.updateStatus(id,request.inventoryStatus()));}
    @PostMapping("/match-ticket-zones/{id}/seat-allocation/debug") public Result<SeatAllocationDebugResponse> debug(@PathVariable Long id,@Valid @RequestBody SeatAllocationRequest request){return Result.success(seatAllocateService.debug(id,request.ticketCount()));}
}
