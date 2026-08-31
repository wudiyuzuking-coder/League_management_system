package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SeasonRequest(@NotBlank @Size(max=80) String seasonName,
                            @NotNull LocalDate startDate,
                            @NotNull LocalDate endDate,
                            @NotNull LocalDateTime registrationStartTime,
                            @NotNull LocalDateTime registrationDeadline,
                            @NotNull @Min(1) @Max(20) Integer maxClubs,
                            @Size(max=500) String description) {}
