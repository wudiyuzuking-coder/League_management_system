package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.vo.PageResponse;

public interface MatchInfoService {
    PageResponse<MatchInfo> list(MatchQueryRequest query);
    MatchInfo getById(Long id);
    MatchInfo create(MatchRequest request);
    MatchInfo update(Long id,MatchRequest request);
    MatchInfo updateStatus(Long id,String status);
    MatchInfo updateScore(Long id,MatchScoreRequest request);
}
