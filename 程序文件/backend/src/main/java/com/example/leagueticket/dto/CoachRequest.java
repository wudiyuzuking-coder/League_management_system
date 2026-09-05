package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachRequest(
        @NotBlank @Size(max = 80) String coachName,
        @NotBlank @Size(max = 50) String title,
        @Size(max = 50) String nationality,
        @Size(max = 500) String description
) {
}
