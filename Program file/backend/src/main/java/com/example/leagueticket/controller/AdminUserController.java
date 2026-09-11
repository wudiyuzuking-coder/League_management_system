package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
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
        if(request.getRoleCode()!=null&&!request.getRoleCode().isBlank()&&!"USER".equals(request.getRoleCode()))throw new com.example.leagueticket.exception.BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,"用户管理只能查询普通USER");
        request.setRoleCode("USER");
        return Result.success(userService.listUsers(request));
    }

    @GetMapping("/{id}")
    public Result<UserResponse> detail(@PathVariable Long id) {
        var user=userService.getById(id);requireUser(user.getRoleCode());return Result.success(UserResponse.from(user));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        var user=userService.getById(id);requireUser(user.getRoleCode());
        if(!java.util.Set.of("ENABLED","DISABLED").contains(request.userStatus()))throw new com.example.leagueticket.exception.BusinessException("用户管理只允许启用或停用USER");
        userService.updateStatus(id, request.userStatus());
        return Result.success();
    }

    private void requireUser(String roleCode){if(!"USER".equals(roleCode))throw new com.example.leagueticket.exception.BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,"用户管理不能操作非USER账号");}
}
