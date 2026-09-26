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
    private LocalDateTime ticketSaleStartTime;
    private Integer maxClubs;
    private String seasonStatus;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private String description;
    private Integer submittedTeamCount;
    private String scheduleBatchStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
