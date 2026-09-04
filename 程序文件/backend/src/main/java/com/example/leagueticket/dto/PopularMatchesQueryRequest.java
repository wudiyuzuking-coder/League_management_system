package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PopularMatchesQueryRequest {
    private Long seasonId;
    @Min(1) @Max(50) private int limit=10;
}
