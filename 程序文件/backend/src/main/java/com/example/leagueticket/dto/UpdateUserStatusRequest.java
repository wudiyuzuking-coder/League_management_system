package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(@NotBlank String userStatus) {
}
