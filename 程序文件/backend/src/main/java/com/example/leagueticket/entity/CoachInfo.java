package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoachInfo {
    private Long coachId;
    private Long clubId;
    private String coachName;
    private String title;
    private String nationality;
    private String description;
    private String coachStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
