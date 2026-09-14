package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@Profile("dev")
@RequiredArgsConstructor
public class AccountController {

    private final SysUserService userService;

    @PostMapping("/cancel")
    public Result<String> cancel(@AuthenticationPrincipal AuthenticatedUser principal) {
        userService.cancelAccount(principal.userId(), principal.roleCode());
        return Result.success("账号已注销");
    }
}
