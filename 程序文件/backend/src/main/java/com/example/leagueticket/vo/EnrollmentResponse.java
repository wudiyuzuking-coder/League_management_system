package com.example.leagueticket.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EnrollmentResponse {
    private Long enrollmentId;
    private Long seasonId;
    private String seasonName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long clubId;
    private String clubName;
    private Long stadiumId;
    private String stadiumName;
    private String enrollmentStatus;
    private LocalDateTime submittedAt;
    private Integer playerCount;
    private Integer coachCount;
    private LocalDateTime nextMatchTime;
    private Long daysUntilNextMatch;
    private List<EnrollmentPlayerResponse> players;
    private List<EnrollmentCoachResponse> coaches;
}
