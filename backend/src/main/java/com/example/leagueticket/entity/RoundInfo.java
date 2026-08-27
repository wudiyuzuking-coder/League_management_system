package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RoundInfo {
    private Long roundId;
    private Long seasonId;
    private Integer roundNo;
    private String roundName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String roundStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
