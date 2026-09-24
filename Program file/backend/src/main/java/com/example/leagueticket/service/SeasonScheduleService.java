package com.example.leagueticket.service;

import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.vo.*;
import java.util.List;

public interface SeasonScheduleService {
    ScheduleDetailResponse generateIfEligible(Long seasonId,String triggerType);
    ScheduleDetailResponse closeRegistrationAndPublishSchedule(Long seasonId,Long confirmedBy);
    ScheduleDetailResponse get(Long seasonId);
    UserSeasonScheduleResponse getPublicConfirmed(Long seasonId);
    PageResponse<SeasonScheduleBatch> list(ScheduleQueryRequest query);
    List<ClubScheduleResponse> clubSchedules(Long clubId);
}
