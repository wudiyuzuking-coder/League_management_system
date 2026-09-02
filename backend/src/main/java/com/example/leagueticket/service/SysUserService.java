package com.example.leagueticket.service;

import com.example.leagueticket.dto.AdminCreateUserRequest;
import com.example.leagueticket.dto.AdminUpdateUserRequest;
import com.example.leagueticket.dto.ChangePasswordRequest;
import com.example.leagueticket.dto.RegisterRequest;
import com.example.leagueticket.dto.UpdateProfileRequest;
import com.example.leagueticket.dto.UserQueryRequest;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.UserResponse;

public interface SysUserService {
    SysUser findByPhone(String phone);
    SysUser getById(Long userId);
    UserResponse register(RegisterRequest request);
    AuthenticatedUser loadAuthenticatedUser(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    PageResponse<UserResponse> listUsers(UserQueryRequest request);
    UserResponse createByAdmin(AdminCreateUserRequest request);
    UserResponse updateByAdmin(Long userId, AdminUpdateUserRequest request);
    void updateStatus(Long userId, String userStatus);
    int initializeDemoPasswords(String rawPassword);
}
