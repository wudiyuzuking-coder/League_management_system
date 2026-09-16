package com.example.leagueticket.service;

import com.example.leagueticket.dto.SeasonRequest;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.vo.PublicSeasonResponse;
import java.util.List;

public interface SeasonInfoService {
    List<SeasonInfo> list();
    List<PublicSeasonResponse> listPublic();
    PublicSeasonResponse getPublicById(Long id);
    SeasonInfo getById(Long id);
    SeasonInfo create(SeasonRequest request);
    SeasonInfo update(Long id, SeasonRequest request);
    SeasonInfo updateStatus(Long id, String status);
}
