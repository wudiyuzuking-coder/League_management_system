package com.example.leagueticket.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class MatchResultReview {private Long matchId;private String reviewStatus;private String reviewReason;private Integer finalHomeScore;private Integer finalAwayScore;private Long confirmedBy;private LocalDateTime confirmedAt;private LocalDateTime createdAt;private LocalDateTime updatedAt;}
