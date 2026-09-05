package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundApplyRequest(
        @NotBlank(message="reason is required") @Size(max=500,message="reason must not exceed 500 characters") String reason) {}
