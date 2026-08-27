package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.service.*;
import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.example.leagueticket.dto.SeatAllocationRequest;
import jakarta.validation.Valid;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Profile("dev") @RequiredArgsConstructor
public class TicketController {
    private final MatchTicketZoneService zoneService;
    private final MatchSeatInventoryService inventoryService;
    private final SeatAllocateService seatAllocateService;
    @GetMapping("/api/matches/{matchId}/ticket-zones") public Result<List<MatchTicketZoneResponse>> list(@PathVariable Long matchId){return Result.success(zoneService.list(matchId));}
    @GetMapping("/api/match-ticket-zones/{id}") public Result<MatchTicketZoneResponse> detail(@PathVariable Long id){return Result.success(zoneService.detail(id));}
    @GetMapping("/api/match-ticket-zones/{id}/availability") public Result<TicketZoneAvailabilityResponse> availability(@PathVariable Long id){return Result.success(inventoryService.availability(id));}
    @PostMapping("/api/match-ticket-zones/{id}/seat-allocation/preview") public Result<SeatAllocationResponse> preview(@PathVariable Long id,@Valid @RequestBody SeatAllocationRequest request){return Result.success(seatAllocateService.preview(id,request.ticketCount()));}
}
