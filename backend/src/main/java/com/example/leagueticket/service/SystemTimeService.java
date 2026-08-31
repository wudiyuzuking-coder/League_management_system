package com.example.leagueticket.service;

import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.SystemTimeResponse;

import java.time.LocalDateTime;

public interface SystemTimeService {
    LocalDateTime now();
    LocalDateTime realNow();
    LocalDateTime getCurrentSystemTime();
    SystemTimeResponse getTime();
    SystemTimeResponse setCurrentSystemTime(LocalDateTime targetTime,AuthenticatedUser operator);
    SystemTimeResponse resetToRealTime(AuthenticatedUser operator);
}
