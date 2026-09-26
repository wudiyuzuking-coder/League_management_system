package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record SeasonCancellationNotificationResponse(
        Long notificationId,
        Long seasonId,
        String seasonName,
        String seasonStatus,
        String notificationType,
        String message,
        String cancelReason,
        LocalDateTime cancelledAt,
        LocalDateTime occurredAt) {
}
