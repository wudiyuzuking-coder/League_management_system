package com.example.leagueticket.service;

import com.example.leagueticket.entity.SeasonInfo;

public interface SeasonLifecycleService {
    SeasonInfo openRegistration(Long seasonId);
    SeasonInfo closeRegistration(Long seasonId);
    SeasonInfo startInProgress(Long seasonId);
    SeasonInfo finish(Long seasonId);
    SeasonInfo transitionCompatible(Long seasonId, String requestedStatus);
}
