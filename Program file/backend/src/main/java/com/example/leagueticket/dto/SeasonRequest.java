package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;

public record SeasonRequest(@NotNull LocalDate startDate,
                            @NotNull @Min(2) @Max(20) Integer maxClubs) {}
