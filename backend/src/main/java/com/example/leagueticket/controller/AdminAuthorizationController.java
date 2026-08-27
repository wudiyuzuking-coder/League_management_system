package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.service.SysPermissionService;
import com.example.leagueticket.service.SysRoleService;
import com.example.leagueticket.vo.PermissionResponse;
import com.example.leagueticket.vo.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Profile("dev")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@RequiredArgsConstructor
public class AdminAuthorizationController {

    private final SysRoleService roleService;
    private final SysPermissionService permissionService;

    @GetMapping("/roles")
    public Result<List<RoleResponse>> roles() {
        return Result.success(roleService.listAll().stream().map(RoleResponse::from).toList());
    }

    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> permissions() {
        return Result.success(permissionService.listAll().stream().map(PermissionResponse::from).toList());
    }
}
