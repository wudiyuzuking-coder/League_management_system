package com.example.leagueticket.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderCreateRequest(@Positive Long matchZoneId,
                                 @Positive Long matchId,
                                 @Pattern(regexp="VIP|NORMAL") String ticketType,
                                 @Positive Integer ticketCount,
                                 @Size(max=4) List<@Positive Long> passengerIds) {}
