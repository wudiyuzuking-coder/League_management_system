package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MatchTicketZoneResponse(Long matchZoneId,Long matchId,Long stadiumZoneId,Long createdBy,
        String zoneName,String zoneCode,BigDecimal price,String zoneStatus,
        LocalDateTime saleStartTime,LocalDateTime saleEndTime,long totalSeatCount,
        long availableSeatCount,long lockedSeatCount,long soldSeatCount,long disabledSeatCount,
        int maxContinuousCount,boolean saleAvailable) {}
