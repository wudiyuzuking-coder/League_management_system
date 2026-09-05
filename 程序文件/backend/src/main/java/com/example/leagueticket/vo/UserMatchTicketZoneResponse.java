package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserMatchTicketZoneResponse(
        Long matchZoneId,
        Long matchId,
        Long stadiumZoneId,
        String zoneName,
        String zoneCode,
        BigDecimal price,
        String zoneStatus,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        long physicalSeatCount,
        long activePhysicalSeatCount,
        int rowCount,
        Integer minSeatNo,
        Integer maxSeatNo,
        long totalSeatCount,
        long availableSeatCount,
        int maxContinuousCount,
        boolean saleAvailable,
        String saleState) {}
