package com.example.leagueticket.vo;
import com.example.leagueticket.algorithm.seat.SeatCandidate;
import java.util.List;
public record SeatAllocationDebugResponse(SeatAllocationResponse best,int maxContinuousCount,List<SeatCandidate> candidates) {}
