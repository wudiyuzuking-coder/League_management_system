package com.example.leagueticket.service;

import com.example.leagueticket.dto.ClubProfileRequest;
import com.example.leagueticket.entity.ClubHomeStadiumConfig;
import com.example.leagueticket.vo.ClubProfileResponse;

public interface ClubHomeStadiumService {
    ClubProfileResponse profile(Long clubId);
    ClubProfileResponse updateProfile(Long clubId, ClubProfileRequest request);
    ClubHomeStadiumConfig requireComplete(Long clubId);
}
