package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record CheckinResponse(Long checkinId, String checkResult, String message,
        String ticketCode, Long ticketId, Long matchId, String matchName,
        String rowLabel, String seatLabel, LocalDateTime checkedAt) {
}
