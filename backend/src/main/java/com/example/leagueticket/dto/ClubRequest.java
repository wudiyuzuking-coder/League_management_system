package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubRequest(
        @NotBlank @Size(max = 100) String clubName,
        @Size(max = 40) String shortName,
        @Size(max = 500) String logoUrl,
        @NotBlank @Size(max = 50) String homeCity,
        @Size(max = 255) String homeAddress,
        Long homeStadiumId,
        @Size(max = 5000) String description
) {
}
