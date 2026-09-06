package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.MatchQueryRequest;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.StadiumInfo;
import com.example.leagueticket.entity.StadiumZone;
import com.example.leagueticket.entity.StadiumSeat;
import com.example.leagueticket.service.MatchInfoService;
import com.example.leagueticket.service.StadiumInfoService;
import com.example.leagueticket.service.StadiumZoneService;
import com.example.leagueticket.service.StadiumSeatService;
import com.example.leagueticket.vo.SeatRowResponse;
import com.example.leagueticket.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Profile("dev") @RequiredArgsConstructor
public class MatchController {
    private final MatchInfoService matchService;
    private final StadiumInfoService stadiumService;
    private final StadiumZoneService zoneService;
    private final StadiumSeatService seatService;
    @GetMapping("/api/matches") public Result<PageResponse<MatchInfo>> matches(@Valid MatchQueryRequest query){return Result.success(matchService.listPublic(query));}
    @GetMapping("/api/matches/{id}") public Result<MatchInfo> match(@PathVariable Long id){return Result.success(matchService.getPublicById(id));}
    @GetMapping("/api/stadiums") public Result<List<StadiumInfo>> stadiums(){return Result.success(stadiumService.list());}
    @GetMapping("/api/stadiums/{id}") public Result<StadiumInfo> stadium(@PathVariable Long id){return Result.success(stadiumService.getById(id));}
    @GetMapping("/api/stadiums/{stadiumId}/zones") public Result<List<StadiumZone>> zones(@PathVariable Long stadiumId){return Result.success(zoneService.list(stadiumId));}
    @GetMapping("/api/stadium-zones/{zoneId}/seats") public Result<List<StadiumSeat>> seats(@PathVariable Long zoneId){return Result.success(seatService.list(zoneId));}
    @GetMapping("/api/stadium-zones/{zoneId}/layout") public Result<List<SeatRowResponse>> layout(@PathVariable Long zoneId){return Result.success(seatService.layout(zoneId));}
}
