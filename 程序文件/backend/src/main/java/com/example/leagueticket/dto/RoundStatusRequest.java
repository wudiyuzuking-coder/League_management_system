package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
public record RoundStatusRequest(@NotBlank String roundStatus) {}
