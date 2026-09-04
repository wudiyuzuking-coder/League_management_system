package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SeasonInfo {
    private Long seasonId;
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationDeadline;
    private Integer maxClubs;
    private String seasonStatus;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
