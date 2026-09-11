package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/club-applications") @Profile("dev")
@PreAuthorize("hasAuthority('USER_MANAGE')") @RequiredArgsConstructor
public class AdminClubApplicationController {
    private final SysUserService users;
    @GetMapping public Result<PageResponse<UserResponse>> list(@Valid UserQueryRequest query){query.setRoleCode("CLUB");query.setUserStatus("PENDING_CLUB_APPROVAL");return Result.success(users.listUsers(query));}
    @PostMapping("/{id}/approve") public Result<UserResponse> approve(@PathVariable Long id,@Valid @RequestBody ClubApprovalRequest request){return Result.success(users.approveClub(id,request));}
}
