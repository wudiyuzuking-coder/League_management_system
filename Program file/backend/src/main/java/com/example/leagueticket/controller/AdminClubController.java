package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.dto.ClubStatusRequest;
import com.example.leagueticket.dto.UpdateUserStatusRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/clubs")
@Profile("dev")
@RequiredArgsConstructor
public class AdminClubController {
    private final ClubInfoService clubService;
    private final SysUserService userService;

    @GetMapping
    public Result<PageResponse<ClubInfo>> clubs(@Valid ClubQueryRequest request) {
        return Result.success(clubService.list(request));
    }

    @GetMapping("/{id}")
    public Result<ClubInfo> club(@PathVariable Long id) {
        return Result.success(clubService.getById(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateClubStatus(@PathVariable Long id, @Valid @RequestBody ClubStatusRequest request) {
        clubService.updateStatus(id, request.clubStatus());
        return Result.success();
    }

    @PutMapping("/{id}/leader-status")
    public Result<Void> updateLeaderStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        if (!Set.of("ENABLED", "DISABLED").contains(request.userStatus())) {
            throw new BusinessException("负责人只允许启用或停用");
        }
        userService.updateStatus(userService.getClubLeader(id).getUserId(), request.userStatus());
        return Result.success();
    }
}
