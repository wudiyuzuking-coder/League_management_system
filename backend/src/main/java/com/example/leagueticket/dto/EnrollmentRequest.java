package com.example.leagueticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EnrollmentRequest(
        @NotNull Long seasonId,
        @NotNull Long stadiumId,
        @NotEmpty @Size(min=11) List<@Valid EnrollmentPlayerRequest> players,
        @NotEmpty List<@NotNull Long> coachIds) {}
