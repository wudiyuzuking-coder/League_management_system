package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class CheckinQueryRequest {
    private Long matchId;
    private Long checkerId;
    private String checkResult;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endTime;
    @Min(1) private int page = 1;
    @Min(1) @Max(100) private int size = 10;
}
