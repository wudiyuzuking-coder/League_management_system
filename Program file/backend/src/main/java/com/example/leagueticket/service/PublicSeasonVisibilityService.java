package com.example.leagueticket.service;

public interface PublicSeasonVisibilityService {
    boolean isPublicVisibleSeason(Long seasonId);
    void requirePublicVisibleSeason(Long seasonId);
}
