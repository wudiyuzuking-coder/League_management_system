package com.example.leagueticket.entity;

import lombok.Data;

@Data
public class ClubSeasonEnrollmentCoach {
    private Long enrollmentCoachId;
    private Long enrollmentId;
    private Long coachId;
    private String coachNameSnapshot;
    private String titleSnapshot;
}
