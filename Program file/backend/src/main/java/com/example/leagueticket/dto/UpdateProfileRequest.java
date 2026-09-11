package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 50) String username,
        @Size(max = 80) String realName,
        @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone
) {
}
