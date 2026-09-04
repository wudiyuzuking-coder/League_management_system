package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotBlank;
public record MatchTicketZoneStatusRequest(@NotBlank String zoneStatus) {}
