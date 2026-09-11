package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClubApprovalRequest(
        @NotBlank @Pattern(regexp = "CREATE_NEW", message = "审核模式仅支持CREATE_NEW") String mode
) {
}
