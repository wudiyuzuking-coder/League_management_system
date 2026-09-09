package com.example.leagueticket.vo;

import java.time.LocalDateTime;

public record CheckinRecordResponse(Long checkinId, Long matchId, String matchName,
        LocalDateTime matchTime, String stadiumName, Long ticketId, String inputTicketCode,
        String ticketStatus, Long checkerId, String checkerUsername, String checkerName,
        String checkResult, String remark, LocalDateTime checkedAt, String zoneName,
        String rowLabel, String seatLabel) {
}
