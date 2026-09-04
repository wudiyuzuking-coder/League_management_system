package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ScheduleQueryRequest(Long seasonId, String batchStatus,
                                   @Min(1) Integer page, @Min(1) @Max(100) Integer size) {
    public int safePage(){return page==null?1:page;}
    public int safeSize(){return size==null?20:size;}
}
