package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderCreateRequest(@NotNull @Positive Long matchZoneId,
                                 @NotNull @Positive Integer ticketCount) {}
