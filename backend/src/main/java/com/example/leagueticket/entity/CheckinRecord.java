package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CheckinRecord {
    private Long checkinId;
    private Long matchId;
    private Long ticketId;
    private String scannedTicketCode;
    private Long checkerId;
    private String checkResult;
    private LocalDateTime checkTime;
    private String remark;
    private String ticketStatus;
    private LocalDateTime usedAt;
    private String checkerUsername;
    private String checkerName;
    private String homeClubName;
    private String awayClubName;
    private LocalDateTime matchTime;
    private String stadiumName;
    private String zoneName;
    private String rowNo;
    private String seatNo;
}
