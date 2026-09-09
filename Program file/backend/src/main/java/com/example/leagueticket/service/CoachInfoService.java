package com.example.leagueticket.service;

import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.entity.CoachInfo;

import java.util.List;

public interface CoachInfoService {
    CoachInfo getById(Long coachId);
    List<CoachInfo> listByClub(Long clubId);
    CoachInfo create(Long clubId, CoachRequest request);
    CoachInfo update(Long clubId, Long coachId, CoachRequest request);
    void updateStatus(Long clubId, Long coachId, String status);
}
