package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.LoginRequest;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.security.JwtService;
import com.example.leagueticket.service.AuthService;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.CurrentUserResponse;
import com.example.leagueticket.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService userService;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.findByPhone(request.phone().trim());
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "手机号不存在");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "密码错误");
        }
        String selectedRole = request.roleCode().trim().toUpperCase();
        if (!selectedRole.equals(user.getRoleCode())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "所选身份与账号不匹配");
        }
        if ("EVENT_ADMIN".equals(selectedRole) || "ADMIN".equals(selectedRole)) {
            if (request.employeeNo() == null || request.employeeNo().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请输入工号");
            }
            if (!request.employeeNo().trim().equals(user.getEmployeeNo())) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "工号与账号不匹配");
            }
        }
        if (!"ENABLED".equals(user.getUserStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号尚未启用");
        }
        AuthenticatedUser principal = userService.loadAuthenticatedUser(user.getUserId());
        userMapper.updateLastLogin(user.getUserId());
        return new LoginResponse(jwtService.createToken(principal), user.getUserId(), user.getUsername(), user.getPhone(),
                user.getRealName(), user.getRoleCode(), user.getClubId());
    }

    @Override
    public CurrentUserResponse currentUser(AuthenticatedUser principal) {
        SysUser user = userService.getById(principal.userId());
        return new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getPhone(), user.getRealName(),
                user.getEmployeeNo(), user.getAvatarUrl(), user.getRoleCode(), user.getClubId(),
                user.getUserStatus(), principal.permissions());
    }
}
