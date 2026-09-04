package com.example.leagueticket.dto;
import jakarta.validation.constraints.*;
public record StadiumSeatRequest(@NotNull @Positive Integer rowNo,
                                 @NotBlank @Size(max=20) String rowLabel,
                                 @NotNull @Positive Integer seatNo,
                                 @NotBlank @Size(max=20) String seatLabel) {}
