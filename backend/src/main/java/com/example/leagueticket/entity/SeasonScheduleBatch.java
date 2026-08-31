package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeasonScheduleBatch {
    private Long batchId;
    private Long seasonId;
    private String seasonName;
    private String batchStatus;
    private String triggerType;
    private Integer clubCount;
    private Integer roundCount;
    private Integer matchCount;
    private LocalDateTime generatedAt;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
