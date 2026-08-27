package com.example.leagueticket.vo;
public record TicketZoneAvailabilityResponse(long totalSeatCount,long availableSeatCount,long lockedSeatCount,
        long soldSeatCount,long disabledSeatCount,int maxContinuousCount) {}
