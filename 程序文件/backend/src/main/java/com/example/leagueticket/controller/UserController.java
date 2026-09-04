package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ChangePasswordRequest;
import com.example.leagueticket.dto.UpdateProfileRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Profile("dev")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @PutMapping
    public Result<UserResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(principal.userId(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.userId(), request);
        return Result.success();
    }
}
