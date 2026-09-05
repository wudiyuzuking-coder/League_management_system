package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank(message = "payMethod is required") String payMethod,
        @NotBlank(message = "simulateResult is required") String simulateResult) {}
