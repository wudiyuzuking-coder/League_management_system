package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ClubApprovalRequest(
        @NotBlank String mode,
        Long existingClubId
) {
}
