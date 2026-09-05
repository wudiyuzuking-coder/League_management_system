package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class MatchQueryRequest {
    @Min(1) private int page=1;
    @Min(1) @Max(100) private int size=10;
    private Long seasonId;
    private Long roundId;
    private Long homeClubId;
    private Long awayClubId;
    private Long clubId;
    private String matchStatus;
    private Boolean publicOnly=false;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startTime;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endTime;
}
