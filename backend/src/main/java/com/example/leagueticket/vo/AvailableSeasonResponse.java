package com.example.leagueticket.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AvailableSeasonResponse {
    private Long seasonId;
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationDeadline;
    private Integer maxClubs;
    private Integer enrolledClubs;
    private Integer remainingSlots;
    private LocalDateTime systemTime;
}
