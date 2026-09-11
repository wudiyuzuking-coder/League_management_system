package com.example.leagueticket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record HomeStadiumRequest(
        @NotBlank @Size(max=50) String city,
        @NotBlank @Size(max=100) String stadiumName,
        @NotBlank @Size(max=255) String address,
        @NotNull @Positive Integer rowsPerZone,
        @NotNull @Positive Integer longSideSeatsPerRow,
        @NotNull @Positive Integer shortSideSeatsPerRow,
        @NotNull @DecimalMin("0.00") BigDecimal vipPrice,
        @NotNull @DecimalMin("0.00") BigDecimal normalPrice) {}
