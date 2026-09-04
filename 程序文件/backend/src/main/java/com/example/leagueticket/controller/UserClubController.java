package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.service.UserClubDetailService;
import com.example.leagueticket.vo.UserClubDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/clubs")
@Profile("dev")
@RequiredArgsConstructor
public class UserClubController {
    private final UserClubDetailService service;

    @GetMapping("/{clubId}")
    public Result<UserClubDetailResponse> detail(@PathVariable Long clubId) {
        return Result.success(service.detail(clubId));
    }
}
