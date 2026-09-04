package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SystemTimeUpdateRequest(@NotNull LocalDateTime targetTime) {}
