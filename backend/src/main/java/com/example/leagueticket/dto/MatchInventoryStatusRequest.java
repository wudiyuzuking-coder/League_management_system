package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotBlank;
public record MatchInventoryStatusRequest(@NotBlank String inventoryStatus) {}
