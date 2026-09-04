package com.example.leagueticket.dto;
import jakarta.validation.constraints.*;
public record StadiumZoneRequest(@NotBlank @Size(max=32) String zoneCode,
                                 @NotBlank @Size(max=80) String zoneName,
                                 @NotNull @PositiveOrZero Integer sortNo,
                                 @Size(max=255) String description) {}
