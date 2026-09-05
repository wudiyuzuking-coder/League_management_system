package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.vo.*;
import java.util.List;

public interface CheckinService {
    List<CheckerMatchResponse> matches(AuthenticatedUser user, CheckerMatchQueryRequest query);
    CheckinResponse checkin(AuthenticatedUser user, Long matchId, CheckinRequest request);
    PageResponse<CheckinRecordResponse> ownRecords(AuthenticatedUser user, CheckinQueryRequest query);
    PageResponse<CheckinRecordResponse> adminRecords(CheckinQueryRequest query);
    CheckinRecordResponse adminDetail(Long id);
}
