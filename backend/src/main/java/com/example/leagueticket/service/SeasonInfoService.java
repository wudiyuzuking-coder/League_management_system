package com.example.leagueticket.service;

import com.example.leagueticket.dto.SeasonRequest;
import com.example.leagueticket.entity.SeasonInfo;
import java.util.List;

public interface SeasonInfoService {
    List<SeasonInfo> list();
    SeasonInfo getById(Long id);
    SeasonInfo create(SeasonRequest request);
    SeasonInfo update(Long id, SeasonRequest request);
    SeasonInfo updateStatus(Long id, String status);
}
