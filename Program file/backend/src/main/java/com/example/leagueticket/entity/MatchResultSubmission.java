package com.example.leagueticket.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class MatchResultSubmission {private Long submissionId;private Long matchId;private Long eventAdminId;private Integer homeScore;private Integer awayScore;private LocalDateTime submittedAt;private String submitterName;}
