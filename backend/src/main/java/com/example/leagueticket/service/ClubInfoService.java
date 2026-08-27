package com.example.leagueticket.service;

import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.dto.ClubRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.vo.PageResponse;

public interface ClubInfoService {
    ClubInfo getById(Long clubId);
    PageResponse<ClubInfo> list(ClubQueryRequest request);
    ClubInfo create(ClubRequest request);
    ClubInfo update(Long clubId, ClubRequest request);
    void updateStatus(Long clubId, String status);
}
