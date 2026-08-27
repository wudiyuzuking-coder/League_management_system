package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SeasonRecordRequest(@NotNull @PositiveOrZero Integer wins,
                                  @NotNull @PositiveOrZero Integer draws,
                                  @NotNull @PositiveOrZero Integer losses,
                                  @NotNull @PositiveOrZero Integer goalsFor,
                                  @NotNull @PositiveOrZero Integer goalsAgainst) {}
