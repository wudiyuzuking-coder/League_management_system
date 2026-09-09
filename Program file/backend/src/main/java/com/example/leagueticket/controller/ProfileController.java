package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.UserAvatarService;
import com.example.leagueticket.vo.AvatarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/avatar")
@Profile("dev")
@RequiredArgsConstructor
public class ProfileController {

    private final UserAvatarService avatarService;

    @PostMapping
    public Result<AvatarResponse> upload(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @RequestParam("file") MultipartFile file) {
        return Result.success(avatarService.upload(principal.userId(), file));
    }

    @DeleteMapping
    public Result<Void> remove(@AuthenticationPrincipal AuthenticatedUser principal) {
        avatarService.remove(principal.userId());
        return Result.success();
    }
}
