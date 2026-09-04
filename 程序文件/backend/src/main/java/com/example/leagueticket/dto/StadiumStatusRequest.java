package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotBlank;
public record StadiumStatusRequest(@NotBlank String stadiumStatus) {}
