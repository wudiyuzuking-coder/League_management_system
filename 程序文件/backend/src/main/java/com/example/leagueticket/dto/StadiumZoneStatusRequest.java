package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotBlank;
public record StadiumZoneStatusRequest(@NotBlank String zoneStatus) {}
