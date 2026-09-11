package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record CoachAdjustRequest(@NotBlank String title) {}
