package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.LoginRequest;
import com.example.leagueticket.dto.RegisterRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.AuthService;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.CurrentUserResponse;
import com.example.leagueticket.vo.LoginResponse;
import com.example.leagueticket.vo.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("dev")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserService userService;

    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Result.success(authService.currentUser(principal));
    }
}
