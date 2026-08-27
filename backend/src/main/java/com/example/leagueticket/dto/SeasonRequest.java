package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SeasonRequest(@NotBlank @Size(max=80) String seasonName,
                            @NotNull LocalDate startDate,
                            @NotNull LocalDate endDate,
                            @Size(max=500) String description) {}
