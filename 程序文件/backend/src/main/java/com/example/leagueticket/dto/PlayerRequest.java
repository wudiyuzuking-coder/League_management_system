package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlayerRequest(
        @NotBlank @Size(max = 80) String playerName,
        @NotNull @Min(1) @Max(99) Integer shirtNo,
        @NotBlank String position,
        @Size(max = 50) String nationality,
        LocalDate birthDate
) {
}
