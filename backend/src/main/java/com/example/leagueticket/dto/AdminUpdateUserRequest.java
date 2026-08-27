package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 80) String realName,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone,
        @NotBlank String roleCode,
        Long clubId,
        @NotBlank String userStatus
) {
}
