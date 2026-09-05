package com.example.leagueticket.service;

import com.example.leagueticket.dto.RoundRequest;
import com.example.leagueticket.entity.RoundInfo;
import java.util.List;

public interface RoundInfoService {
    List<RoundInfo> listBySeason(Long seasonId);
    RoundInfo getById(Long id);
    RoundInfo create(Long seasonId, RoundRequest request);
    RoundInfo update(Long id, RoundRequest request);
    RoundInfo updateStatus(Long id, String status);
}
