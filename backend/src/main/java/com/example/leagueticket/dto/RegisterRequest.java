package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 4, max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "must contain only letters, numbers, or underscores") String username,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 32) String roleCode,
        @Size(max = 80) String realName,
        @Size(max = 100) String clubName,
        @Size(max = 50) String employeeNo
) {
}
