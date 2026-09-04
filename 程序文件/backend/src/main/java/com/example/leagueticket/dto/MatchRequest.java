package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MatchRequest(@NotNull Long seasonId,@NotNull Long roundId,
                           @NotNull Long homeClubId,@NotNull Long awayClubId,
                           @NotNull Long stadiumId,@NotNull LocalDateTime matchTime) {}
