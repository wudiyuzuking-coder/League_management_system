package com.example.leagueticket.dto;

import jakarta.validation.constraints.*;
public record StadiumRequest(@NotBlank @Size(max=100) String stadiumName,
                             @NotBlank @Size(max=50) String city,
                             @NotBlank @Size(max=255) String address,
                             @NotNull @Positive Integer capacity,
                             @Size(max=500) String layoutDesc) {}
