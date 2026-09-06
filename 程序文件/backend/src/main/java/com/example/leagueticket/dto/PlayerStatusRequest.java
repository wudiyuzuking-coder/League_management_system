package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record PlayerStatusRequest(@NotBlank String playerStatus) {
}
