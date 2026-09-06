package com.example.leagueticket.service;

import com.example.leagueticket.dto.PlayerSeasonStatRequest;
import com.example.leagueticket.entity.PlayerSeasonStat;

import java.util.List;

public interface PlayerSeasonStatService {
    PlayerSeasonStat getById(Long statId);
    List<PlayerSeasonStat> listByClub(Long clubId);
    PlayerSeasonStat create(Long clubId, PlayerSeasonStatRequest request);
    PlayerSeasonStat update(Long clubId, Long statId, PlayerSeasonStatRequest request);
}
