package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.StadiumCapacityResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin") @Profile("dev") @RequiredArgsConstructor
public class AdminStadiumController {
    private final StadiumInfoService stadiumService;private final StadiumZoneService zoneService;private final StadiumSeatService seatService;
    @GetMapping("/stadiums") public Result<List<StadiumInfo>> stadiums(@RequestParam(required=false) String name,@RequestParam(required=false) String city){return Result.success(stadiumService.search(name,city));}
    @GetMapping("/stadiums/{id}") public Result<StadiumInfo> stadium(@PathVariable Long id){return Result.success(stadiumService.getById(id));}
    @PostMapping("/stadiums") public Result<StadiumInfo> createStadium(@Valid @RequestBody StadiumRequest request){return Result.success(stadiumService.create(request));}
    @PutMapping("/stadiums/{id}") public Result<StadiumInfo> updateStadium(@PathVariable Long id,@Valid @RequestBody StadiumRequest request){return Result.success(stadiumService.update(id,request));}
    @PutMapping("/stadiums/{id}/status") public Result<StadiumInfo> stadiumStatus(@PathVariable Long id,@Valid @RequestBody StadiumStatusRequest request){return Result.success(stadiumService.updateStatus(id,request.stadiumStatus()));}
    @GetMapping("/stadiums/{id}/capacity-summary") public Result<StadiumCapacityResponse> capacity(@PathVariable Long id){return Result.success(stadiumService.capacitySummary(id));}
    @GetMapping("/stadiums/{stadiumId}/zones") public Result<List<StadiumZone>> zones(@PathVariable Long stadiumId){return Result.success(zoneService.list(stadiumId));}
    @PostMapping("/stadiums/{stadiumId}/zones") public Result<StadiumZone> createZone(@PathVariable Long stadiumId,@Valid @RequestBody StadiumZoneRequest request){return Result.success(zoneService.create(stadiumId,request));}
    @PutMapping("/stadium-zones/{id}") public Result<StadiumZone> updateZone(@PathVariable Long id,@Valid @RequestBody StadiumZoneRequest request){return Result.success(zoneService.update(id,request));}
    @PutMapping("/stadium-zones/{id}/status") public Result<StadiumZone> zoneStatus(@PathVariable Long id,@Valid @RequestBody StadiumZoneStatusRequest request){return Result.success(zoneService.updateStatus(id,request.zoneStatus()));}
    @GetMapping("/stadium-zones/{zoneId}/seats") public Result<List<StadiumSeat>> seats(@PathVariable Long zoneId){return Result.success(seatService.list(zoneId));}
    @PostMapping("/stadium-zones/{zoneId}/seats") public Result<StadiumSeat> createSeat(@PathVariable Long zoneId,@Valid @RequestBody StadiumSeatRequest request){return Result.success(seatService.create(zoneId,request));}
    @PostMapping("/stadium-zones/{zoneId}/seats/batch") public Result<Integer> batch(@PathVariable Long zoneId,@Valid @RequestBody SeatBatchRequest request){return Result.success(seatService.batchCreate(zoneId,request));}
    @PutMapping("/stadium-seats/{id}") public Result<StadiumSeat> updateSeat(@PathVariable Long id,@Valid @RequestBody StadiumSeatRequest request){return Result.success(seatService.update(id,request));}
    @PutMapping("/stadium-seats/{id}/status") public Result<StadiumSeat> seatStatus(@PathVariable Long id,@Valid @RequestBody StadiumSeatStatusRequest request){return Result.success(seatService.updateStatus(id,request.seatStatus()));}
}
