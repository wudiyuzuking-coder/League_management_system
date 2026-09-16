package com.example.leagueticket.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PublicSeasonResponse {
    private Long seasonId;
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer teamCount;
    private Integer roundCount;
    private Integer matchCount;
    private boolean scheduleConfirmed;
    private String publicStatus;
}
