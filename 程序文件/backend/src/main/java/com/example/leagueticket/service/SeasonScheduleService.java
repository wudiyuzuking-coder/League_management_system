package com.example.leagueticket.service;

import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.vo.*;
import java.util.List;

public interface SeasonScheduleService {
    ScheduleDetailResponse generateIfEligible(Long seasonId,String triggerType);
    ScheduleDetailResponse get(Long seasonId);
    PageResponse<SeasonScheduleBatch> list(ScheduleQueryRequest query);
    ScheduleDetailResponse confirm(Long seasonId,Long userId);
    List<ClubScheduleResponse> clubSchedules(Long clubId);
}
