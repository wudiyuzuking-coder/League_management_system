package com.example.leagueticket.dto;
import jakarta.validation.constraints.NotBlank;
public record StadiumSeatStatusRequest(@NotBlank String seatStatus) {}
