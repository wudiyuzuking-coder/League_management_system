package com.example.leagueticket.service;

import com.example.leagueticket.dto.LoginRequest;
import com.example.leagueticket.dto.ManagementAccountProbeRequest;
import com.example.leagueticket.dto.ManagementActivationRequest;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.CurrentUserResponse;
import com.example.leagueticket.vo.LoginResponse;
import com.example.leagueticket.vo.ManagementAccountStateResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    ManagementAccountStateResponse probeManagementAccount(ManagementAccountProbeRequest request);
    LoginResponse activateManagementAccount(ManagementActivationRequest request);
    CurrentUserResponse currentUser(AuthenticatedUser principal);
}
