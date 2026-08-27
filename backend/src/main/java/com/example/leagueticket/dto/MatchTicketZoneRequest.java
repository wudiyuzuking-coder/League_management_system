package com.example.leagueticket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MatchTicketZoneRequest(
        @NotNull @Positive Long stadiumZoneId,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull LocalDateTime saleStartTime,
        @NotNull LocalDateTime saleEndTime) {}
