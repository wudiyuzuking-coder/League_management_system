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
    private Long awayClubId;
    private String awayClubName;
    private Long stadiumId;
    private String stadiumName;
    private String matchStatus;
}
