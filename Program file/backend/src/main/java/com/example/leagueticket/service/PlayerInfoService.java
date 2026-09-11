package com.example.leagueticket.service;

import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.dto.PlayerAdjustRequest;
import com.example.leagueticket.dto.PlayerReturnRequest;
import com.example.leagueticket.entity.PlayerInfo;

import java.util.List;

public interface PlayerInfoService {
    PlayerInfo getById(Long playerId);
    List<PlayerInfo> listByClub(Long clubId);
    PlayerInfo create(Long clubId, PlayerRequest request);
    PlayerInfo update(Long clubId, Long playerId, PlayerRequest request);
    PlayerInfo adjust(Long clubId, Long playerId, PlayerAdjustRequest request);
    PlayerInfo returnToTeam(Long clubId, Long playerId, PlayerReturnRequest request);
    void updateStatus(Long clubId, Long playerId, String status);
    void cleanup(Long clubId, Long playerId);
}
