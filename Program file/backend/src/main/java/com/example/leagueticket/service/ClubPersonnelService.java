package com.example.leagueticket.service;

import com.example.leagueticket.vo.PersonnelOverviewResponse;

public interface ClubPersonnelService {
    PersonnelOverviewResponse overview(Long clubId);
    PersonnelOverviewResponse requireCompliant(Long clubId);
}
