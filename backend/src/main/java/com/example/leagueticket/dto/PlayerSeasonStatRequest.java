package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PlayerSeasonStatRequest(
        @NotNull Long seasonId,
        @NotNull Long playerId,
        @NotNull @PositiveOrZero Integer appearances,
        @NotNull @PositiveOrZero Integer goals,
        @NotNull @PositiveOrZero Integer assists
) {
}
