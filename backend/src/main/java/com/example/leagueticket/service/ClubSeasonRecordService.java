package com.example.leagueticket.service;

import com.example.leagueticket.dto.SeasonRecordRequest;
import com.example.leagueticket.vo.StandingResponse;
import java.util.List;

public interface ClubSeasonRecordService {
    List<StandingResponse> standings(Long seasonId);
    int initialize(Long seasonId);
    StandingResponse update(Long recordId, SeasonRecordRequest request);
}
