package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnrollmentPlayerRequest(
        @NotNull Long playerId,
        @NotNull @Pattern(regexp="STARTER|SUBSTITUTE") String lineupRole) {}
