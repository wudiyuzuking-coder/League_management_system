package com.example.leagueticket.service;

public interface LifecycleCompensationService {
    void catchUpAfterSystemTimeChange();
    int openDueRegistrations();
    int closeDueRegistrations();
    int startDueSeasons();
    int startDueMatches();
    int closeExpiredOrders();
}
