package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record CoachStatusRequest(@NotBlank String coachStatus) {
}
