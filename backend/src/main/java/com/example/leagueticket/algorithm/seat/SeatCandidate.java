package com.example.leagueticket.algorithm.seat;

import java.util.List;

public record SeatCandidate(int rowNo,String rowLabel,int startSeatNo,int endSeatNo,
        double centerDistance,int remainingFragmentCount,int maxRemainingContinuousLength,
        List<Long> inventoryIds,List<Long> seatIds,List<Integer> seatNos,List<String> seatLabels,
        String candidateScore) {}
