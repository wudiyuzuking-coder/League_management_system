package com.example.leagueticket.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduleMatchResponse {
    private Long matchId;
    private Integer roundNo;
    private LocalDateTime matchDateTime;
    private Long homeClubId;
    private String homeClubName;
    private String homeLogoUrl;
    private Long awayClubId;
    private String awayClubName;
    private String awayLogoUrl;
    private Long stadiumId;
    private String stadiumName;
    private String stadiumAddress;
    private String matchStatus;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private long remainingTickets;
    private int onSaleZoneCount;
    private String saleStatus;
    private boolean purchasable;
}
