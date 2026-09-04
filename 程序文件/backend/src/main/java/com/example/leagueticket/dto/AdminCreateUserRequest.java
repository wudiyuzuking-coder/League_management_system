package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank @Size(min = 2, max = 50) String username,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 80) String realName,
        @NotBlank String roleCode,
        @Size(max = 16) String employeeNo,
        Long clubId
) {
}
