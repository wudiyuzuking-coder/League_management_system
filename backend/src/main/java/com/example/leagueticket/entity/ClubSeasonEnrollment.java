package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ClubSeasonEnrollment {
    private Long enrollmentId;
    private Long seasonId;
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationDeadline;
    private Integer maxClubs;
    private Long clubId;
    private String clubName;
    private Long stadiumId;
    private String stadiumName;
    private String enrollmentStatus;
    private LocalDateTime submittedAt;
    private Integer playerCount;
    private Integer coachCount;
    private LocalDateTime nextMatchTime;
}
