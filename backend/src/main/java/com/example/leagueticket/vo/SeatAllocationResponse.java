package com.example.leagueticket.vo;

import com.example.leagueticket.algorithm.seat.SeatCandidate;
import java.util.List;

public record SeatAllocationResponse(Long matchZoneId,Long matchId,int ticketCount,int rowNo,String rowLabel,
        List<Long> seatIds,List<Long> inventoryIds,List<Integer> seatNos,List<String> seatLabels,String candidateScore) {
    public static SeatAllocationResponse from(Long matchZoneId,Long matchId,int count,SeatCandidate c){return new SeatAllocationResponse(matchZoneId,matchId,count,c.rowNo(),c.rowLabel(),c.seatIds(),c.inventoryIds(),c.seatNos(),c.seatLabels(),c.candidateScore());}
}
