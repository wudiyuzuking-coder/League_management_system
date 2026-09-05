package com.example.leagueticket.service;

import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.entity.PlayerInfo;

import java.util.List;

public interface PlayerInfoService {
    PlayerInfo getById(Long playerId);
    List<PlayerInfo> listByClub(Long clubId);
    PlayerInfo create(Long clubId, PlayerRequest request);
    PlayerInfo update(Long clubId, Long playerId, PlayerRequest request);
    void updateStatus(Long clubId, Long playerId, String status);
}
