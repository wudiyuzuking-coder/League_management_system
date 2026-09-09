package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.MatchResultReminderResponse;

public interface MatchInfoService {
    PageResponse<MatchInfo> list(MatchQueryRequest query);
    PageResponse<MatchInfo> listPublic(MatchQueryRequest query);
    MatchInfo getById(Long id);
    MatchInfo getPublicById(Long id);
    MatchInfo create(MatchRequest request);
    MatchInfo update(Long id,MatchRequest request);
    MatchInfo updateStatus(Long id,String status);
    MatchInfo updateScore(Long id,MatchScoreRequest request);
    PageResponse<MatchResultReminderResponse> resultReminders(MatchResultReminderQueryRequest query);
}
