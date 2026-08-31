package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.AdminCreateUserRequest;
import com.example.leagueticket.dto.AdminUpdateUserRequest;
import com.example.leagueticket.dto.ChangePasswordRequest;
import com.example.leagueticket.dto.RegisterRequest;
import com.example.leagueticket.dto.UpdateProfileRequest;
import com.example.leagueticket.dto.UserQueryRequest;
import com.example.leagueticket.entity.SysRole;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SysRolePermissionService;
import com.example.leagueticket.service.SysRoleService;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private static final Set<String> USER_STATUSES = Set.of("ENABLED", "DISABLED", "LOCKED");
    private static final Set<String> CLUB_BOUND_ROLES = Set.of("CLUB");
    private static final Set<String> UNBOUND_ROLES = Set.of("USER", "EVENT_ADMIN", "ADMIN");
    private static final Set<String> PUBLIC_REGISTER_ROLES = Set.of("USER", "CLUB", "EVENT_ADMIN", "ADMIN");

    private final SysUserMapper userMapper;
    private final SysRoleService roleService;
    private final SysRolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public SysUser getById(Long userId) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "user not found");
        }
        return user;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String roleCode = normalizeRegisterRole(request.roleCode());
        SysRole role = roleService.getByCode(roleCode);
        String displayName = registerDisplayName(roleCode, request);
        String status = "USER".equals(roleCode) ? "ENABLED" : "DISABLED";
        SysUser user = buildUser(request.username(), request.phone(), request.password(), displayName,
                role, null, status);
        userMapper.insert(user);
        return UserResponse.from(userMapper.findById(user.getUserId()));
    }

    @Override
    public AuthenticatedUser loadAuthenticatedUser(Long userId) {
        SysUser user = getById(userId);
        if (!"ENABLED".equals(user.getUserStatus())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "account is not enabled");
        }
        return AuthenticatedUser.from(user, rolePermissionService.listPermissionCodes(user.getRoleId()));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        getById(userId);
        assertPhoneAvailable(request.phone(), userId);
        userMapper.updateProfile(userId, request.realName().trim(), request.phone());
        return UserResponse.from(userMapper.findById(userId));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = getById(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("original password is incorrect");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    public PageResponse<UserResponse> listUsers(UserQueryRequest request) {
        validateOptionalFilters(request.getRoleCode(), request.getUserStatus());
        long total = userMapper.countPage(request.getUsername(), request.getPhone(), request.getRoleCode(), request.getUserStatus());
        long offset = (long) (request.getPage() - 1) * request.getSize();
        List<UserResponse> records = userMapper.findPage(request.getUsername(), request.getPhone(), request.getRoleCode(),
                        request.getUserStatus(), offset, request.getSize())
                .stream().map(UserResponse::from).toList();
        return new PageResponse<>(records, total, request.getPage(), request.getSize());
    }

    @Override
    @Transactional
    public UserResponse createByAdmin(AdminCreateUserRequest request) {
        SysRole role = roleService.getByCode(request.roleCode());
        validateRoleClub(role.getRoleCode(), request.clubId());
        SysUser user = buildUser(request.username(), request.phone(), request.password(), request.realName(),
                role, request.clubId(), "ENABLED");
        userMapper.insert(user);
        return UserResponse.from(userMapper.findById(user.getUserId()));
    }

    @Override
    @Transactional
    public UserResponse updateByAdmin(Long userId, AdminUpdateUserRequest request) {
        SysUser user = getById(userId);
        SysRole role = roleService.getByCode(request.roleCode());
        validateStatus(request.userStatus());
        validateRoleClub(role.getRoleCode(), request.clubId());
        assertPhoneAvailable(request.phone(), userId);
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone());
        user.setRoleId(role.getRoleId());
        user.setClubId(request.clubId());
        user.setUserStatus(request.userStatus());
        userMapper.updateByAdmin(user);
        return UserResponse.from(userMapper.findById(userId));
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, String userStatus) {
        SysUser user = getById(userId);
        validateStatus(userStatus);
        if ("ENABLED".equals(userStatus)
                && "CLUB".equals(user.getRoleCode())
                && user.getClubId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "CLUB账号启用前必须先绑定俱乐部");
        }
        userMapper.updateStatus(userId, userStatus);
    }

    @Override
    @Transactional
    public int initializeDemoPasswords(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 72) {
            throw new IllegalArgumentException("demo password length must be between 6 and 72 characters");
        }
        return userMapper.initializeDemoPasswords(passwordEncoder.encode(rawPassword));
    }

    private SysUser buildUser(String username, String phone, String rawPassword, String realName,
                              SysRole role, Long clubId, String status) {
        assertUsernameAvailable(username, null);
        assertPhoneAvailable(phone, null);
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRealName(realName.trim());
        user.setRoleId(role.getRoleId());
        user.setRoleCode(role.getRoleCode());
        user.setClubId(clubId);
        user.setUserStatus(status);
        return user;
    }

    private void assertUsernameAvailable(String username, Long excludeId) {
        if (userMapper.countByUsername(username.trim(), excludeId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "username already exists");
        }
    }

    private void assertPhoneAvailable(String phone, Long excludeId) {
        if (userMapper.countByPhone(phone, excludeId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "phone already exists");
        }
    }

    private String normalizeRegisterRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException("请选择注册身份");
        }
        String value = roleCode.trim().toUpperCase();
        if (!PUBLIC_REGISTER_ROLES.contains(value)) {
            throw new BusinessException("请选择正确的注册身份");
        }
        return value;
    }

    private String registerDisplayName(String roleCode, RegisterRequest request) {
        return switch (roleCode) {
            case "USER" -> requiredText(request.realName(), "用户姓名不能为空");
            case "CLUB" -> requiredText(request.clubName(), "俱乐部名称不能为空");
            case "EVENT_ADMIN", "ADMIN" -> requiredText(request.employeeNo(), "工号不能为空");
            default -> throw new BusinessException("请选择正确的注册身份");
        };
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private void validateRoleClub(String roleCode, Long clubId) {
        if (CLUB_BOUND_ROLES.contains(roleCode) && clubId == null) {
            throw new BusinessException("俱乐部负责人账号必须绑定俱乐部");
        }
        if (UNBOUND_ROLES.contains(roleCode) && clubId != null) {
            throw new BusinessException("普通用户、赛事管理员和系统管理员账号不能绑定俱乐部");
        }
        if (!CLUB_BOUND_ROLES.contains(roleCode) && !UNBOUND_ROLES.contains(roleCode)) {
            throw new BusinessException("不支持的角色类型");
        }
    }

    private void validateStatus(String status) {
        if (!USER_STATUSES.contains(status)) {
            throw new BusinessException("invalid user status");
        }
    }

    private void validateOptionalFilters(String roleCode, String status) {
        if (roleCode != null && !roleCode.isBlank()) {
            roleService.getByCode(roleCode);
        }
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
    }
}
