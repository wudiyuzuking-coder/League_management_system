package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone,
        @NotBlank String password
) {
}
