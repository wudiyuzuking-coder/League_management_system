package com.example.leagueticket.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class CheckerMatchQueryRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endDate;
    private String matchStatus;
}
