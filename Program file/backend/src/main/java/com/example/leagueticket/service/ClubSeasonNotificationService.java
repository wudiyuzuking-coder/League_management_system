package com.example.leagueticket.service;

import com.example.leagueticket.vo.SeasonCancellationNotificationResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ClubSeasonNotificationService {
    int createCancellationNotifications(Long seasonId, LocalDateTime occurredAt);
    List<SeasonCancellationNotificationResponse> listForClub(Long clubId);
}
