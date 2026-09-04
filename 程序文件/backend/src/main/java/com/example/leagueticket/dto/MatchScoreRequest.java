package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
public record MatchScoreRequest(@NotNull @PositiveOrZero Integer homeScore,
                                @NotNull @PositiveOrZero Integer awayScore) {}
