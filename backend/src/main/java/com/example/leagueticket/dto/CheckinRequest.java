package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckinRequest(
        @NotBlank(message = "ticketCode is required")
        @Size(max = 64, message = "ticketCode must not exceed 64 characters")
        String ticketCode) {
}
