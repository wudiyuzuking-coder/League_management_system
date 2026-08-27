package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record SeatAllocationRequest(@NotNull @Positive Integer ticketCount) {}
