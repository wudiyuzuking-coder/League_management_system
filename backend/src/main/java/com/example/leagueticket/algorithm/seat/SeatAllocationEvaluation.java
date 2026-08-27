package com.example.leagueticket.algorithm.seat;
import java.util.List;
public record SeatAllocationEvaluation(int maxContinuousCount,List<SeatCandidate> candidates) {
    public SeatCandidate best(){return candidates.isEmpty()?null:candidates.get(0);}
}
