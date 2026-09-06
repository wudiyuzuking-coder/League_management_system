package com.example.leagueticket.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClubScheduleResponse {
    private Long matchId;
    private Long seasonId;
    private String seasonName;
    private Integer roundNo;
    private Boolean home;
    private Long opponentClubId;
    private String opponentClubName;
    private LocalDateTime matchDateTime;
    private Long stadiumId;
    private String stadiumName;
    private Long daysUntilMatch;
}
