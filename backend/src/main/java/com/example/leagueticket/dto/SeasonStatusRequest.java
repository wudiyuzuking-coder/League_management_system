package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
public record SeasonStatusRequest(@NotBlank String seasonStatus) {}
