package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record CoachRequest(
        @NotBlank @Size(max = 80) String coachName,
        @NotBlank @Size(max = 50) String title,
        @Size(max = 50) String nationality,
        @Min(1900) @Max(2100) Integer birthYear,
        @Size(max = 500) String description
) {
}
