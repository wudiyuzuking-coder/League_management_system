package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@Profile("dev")
@RequiredArgsConstructor
public class PublicClubController {
    private final ClubInfoService clubService;

    @GetMapping
    public Result<PageResponse<ClubInfo>> list(@Valid ClubQueryRequest request) {
        return Result.success(clubService.list(request));
    }

    @GetMapping("/{id}")
    public Result<ClubInfo> detail(@PathVariable Long id) {
        return Result.success(clubService.getById(id));
    }
}
