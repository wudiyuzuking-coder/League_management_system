package com.example.leagueticket.algorithm.seat;

public record SeatPosition(Long inventoryId,Long stadiumSeatId,int rowNo,String rowLabel,
        int seatNo,String seatLabel,String inventoryStatus,int physicalMinSeatNo,
        int physicalMaxSeatNo,int version) {}
