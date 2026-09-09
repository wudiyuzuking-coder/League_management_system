package com.example.leagueticket.service;

import com.example.leagueticket.vo.UserClubDetailResponse;

public interface UserClubDetailService {
    UserClubDetailResponse detail(Long clubId);
}
