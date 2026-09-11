package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController @RequestMapping("/api/admin/internal-users") @Profile("dev")
@PreAuthorize("hasAuthority('USER_MANAGE')") @RequiredArgsConstructor
public class AdminInternalUserController {
    private static final Set<String> ROLES=Set.of("EVENT_ADMIN","ADMIN");
    private final SysUserService users;
    @GetMapping public Result<PageResponse<UserResponse>> list(@Valid UserQueryRequest query){requireRole(query.getRoleCode());return Result.success(users.listUsers(query));}
    @GetMapping("/{id}") public Result<UserResponse> detail(@PathVariable Long id){var user=users.getById(id);requireRole(user.getRoleCode());return Result.success(UserResponse.from(user));}
    @PostMapping public Result<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request){requireRole(request.roleCode());return Result.success(users.createByAdmin(request));}
    @PutMapping("/{id}/status") public Result<Void> status(@PathVariable Long id,@Valid @RequestBody UpdateUserStatusRequest request){var user=users.getById(id);requireRole(user.getRoleCode());if("PENDING_ACTIVATION".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.CONFLICT,"待首次启用账号不能由后台直接启用");if(!Set.of("ENABLED","DISABLED").contains(request.userStatus()))throw new BusinessException("内部人员只允许启用或停用");users.updateStatus(id,request.userStatus());return Result.success();}
    private void requireRole(String role){if(role==null||!ROLES.contains(role.trim().toUpperCase()))throw new BusinessException(HttpStatus.FORBIDDEN,"内部人员管理只能操作EVENT_ADMIN或ADMIN");}
}
