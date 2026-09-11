package com.example.leagueticket.dto;

import jakarta.validation.constraints.*;

public record PlayerAdjustRequest(
        @NotNull @Min(1) @Max(99) Integer shirtNo,
        @NotBlank String position,
        @NotBlank String lineupRole
) {}
