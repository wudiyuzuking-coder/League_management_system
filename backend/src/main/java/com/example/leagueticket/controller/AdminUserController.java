package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.AdminCreateUserRequest;
import com.example.leagueticket.dto.AdminUpdateUserRequest;
import com.example.leagueticket.dto.UpdateUserStatusRequest;
import com.example.leagueticket.dto.UserQueryRequest;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Profile("dev")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService userService;

    @GetMapping
    public Result<PageResponse<UserResponse>> list(@Valid UserQueryRequest request) {
        return Result.success(userService.listUsers(request));
    }

    @GetMapping("/{id}")
    public Result<UserResponse> detail(@PathVariable Long id) {
        return Result.success(UserResponse.from(userService.getById(id)));
    }

    @PostMapping
    public Result<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return Result.success(userService.createByAdmin(request));
    }

    @PutMapping("/{id}")
    public Result<UserResponse> update(@PathVariable Long id,
                                       @Valid @RequestBody AdminUpdateUserRequest request) {
        return Result.success(userService.updateByAdmin(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateStatus(id, request.userStatus());
        return Result.success();
    }
}
