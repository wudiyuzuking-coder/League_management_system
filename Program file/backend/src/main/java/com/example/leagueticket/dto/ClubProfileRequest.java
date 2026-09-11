package com.example.leagueticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ClubProfileRequest(
        @NotBlank @Size(max=100) String clubName,
        @Size(max=40) String shortName,
        @Size(max=500) String logoUrl,
        @Size(max=2000) String description,
        @NotNull @Valid HomeStadiumRequest homeStadium) {}
