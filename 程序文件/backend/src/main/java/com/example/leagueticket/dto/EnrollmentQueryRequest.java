package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record EnrollmentQueryRequest(
        Long seasonId,
        Long clubId,
        @Pattern(regexp="SUBMITTED") String enrollmentStatus,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size) {
    public int safePage(){return page==null?1:page;}
    public int safeSize(){return size==null?20:size;}
}
