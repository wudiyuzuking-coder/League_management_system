package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ClubStatusRequest(@NotBlank String clubStatus) {
}
