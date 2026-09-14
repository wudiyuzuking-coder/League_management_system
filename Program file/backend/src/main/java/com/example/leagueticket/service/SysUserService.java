package com.example.leagueticket.service;

import com.example.leagueticket.dto.AdminCreateUserRequest;
import com.example.leagueticket.dto.ChangePasswordRequest;
import com.example.leagueticket.dto.ClubApprovalRequest;
import com.example.leagueticket.dto.RegisterRequest;
import com.example.leagueticket.dto.UpdateProfileRequest;
import com.example.leagueticket.dto.UserQueryRequest;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.UserResponse;

public interface SysUserService {
    SysUser findByPhoneAndRole(String phone, String roleCode);
    boolean phoneExists(String phone);
    SysUser getById(Long userId);
    SysUser getClubLeader(Long clubId);
    UserResponse register(RegisterRequest request);
    AuthenticatedUser loadAuthenticatedUser(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void cancelAccount(Long userId, String roleCode);
    PageResponse<UserResponse> listUsers(UserQueryRequest request);
    UserResponse createByAdmin(AdminCreateUserRequest request);
    UserResponse approveClub(Long userId, ClubApprovalRequest request);
    void updateStatus(Long userId, String userStatus);
    int initializeDemoPasswords(String rawPassword);
}
