package com.example.leagueticket.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class ClubScheduleResponse {
    private Long matchId;
    private Long seasonId;
    private String seasonName;
    private Integer roundNo;
    private Boolean home;
    private Long opponentClubId;
    private String opponentClubName;
    private String opponentLogoUrl;
    private LocalDateTime matchDateTime;
    private Long stadiumId;
    private String stadiumName;
    private Long daysUntilMatch;
    private String matchStatus;
    private Integer ownScore;
    private Integer opponentScore;
    private Long soldVipCount;
    private Long soldNormalCount;
    private BigDecimal totalRevenue;
    private BigDecimal clubRevenue;
}
