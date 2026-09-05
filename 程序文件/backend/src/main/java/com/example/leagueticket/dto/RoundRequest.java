package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RoundRequest(@NotNull @Positive Integer roundNo,
                           @NotBlank @Size(max=80) String roundName,
                           @NotNull LocalDate startDate,
                           @NotNull LocalDate endDate) {}
