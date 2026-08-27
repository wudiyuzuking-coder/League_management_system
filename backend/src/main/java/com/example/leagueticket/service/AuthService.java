package com.example.leagueticket.service;

import com.example.leagueticket.dto.LoginRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.CurrentUserResponse;
import com.example.leagueticket.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    CurrentUserResponse currentUser(AuthenticatedUser principal);
}
