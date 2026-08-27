package com.example.leagueticket.algorithm.seat;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SeatAllocationAlgorithm {
    public static final Comparator<SeatCandidate> CANDIDATE_ORDER=Comparator
            .comparingInt(SeatCandidate::rowNo)
            .thenComparingDouble(SeatCandidate::centerDistance)
            .thenComparingInt(SeatCandidate::remainingFragmentCount)
            .thenComparing(Comparator.comparingInt(SeatCandidate::maxRemainingContinuousLength).reversed())
            .thenComparingInt(SeatCandidate::startSeatNo);

    public SeatAllocationEvaluation evaluate(List<SeatPosition> source,int ticketCount){
        List<SeatPosition> available=source.stream().filter(s->"AVAILABLE".equals(s.inventoryStatus()))
                .sorted(Comparator.comparingInt(SeatPosition::rowNo).thenComparingInt(SeatPosition::seatNo)).toList();
        Map<Integer,List<SeatPosition>> rows=new LinkedHashMap<>();
        available.forEach(s->rows.computeIfAbsent(s.rowNo(),ignored->new ArrayList<>()).add(s));
        List<SeatCandidate> candidates=new ArrayList<>();int maxContinuous=0;
        for(List<SeatPosition> row:rows.values()){
            for(List<SeatPosition> segment:segments(row)){
                maxContinuous=Math.max(maxContinuous,segment.size());
                for(int start=0;start+ticketCount<=segment.size();start++)candidates.add(candidate(segment,start,ticketCount));
            }
        }
        candidates.sort(CANDIDATE_ORDER);
        return new SeatAllocationEvaluation(maxContinuous,List.copyOf(candidates));
    }

    public int maxContinuous(List<SeatPosition> source){return evaluate(source,Integer.MAX_VALUE).maxContinuousCount();}

    private List<List<SeatPosition>> segments(List<SeatPosition> row){
        List<List<SeatPosition>> result=new ArrayList<>();List<SeatPosition> current=new ArrayList<>();Integer previous=null;
        for(SeatPosition seat:row){if(previous!=null&&seat.seatNo()!=previous+1){result.add(current);current=new ArrayList<>();}current.add(seat);previous=seat.seatNo();}
        if(!current.isEmpty())result.add(current);return result;
    }

    private SeatCandidate candidate(List<SeatPosition> segment,int start,int count){
        List<SeatPosition> window=segment.subList(start,start+count);SeatPosition first=window.get(0),last=window.get(window.size()-1);
        double rowCenter=(first.physicalMinSeatNo()+first.physicalMaxSeatNo())/2.0;
        double candidateCenter=(first.seatNo()+last.seatNo())/2.0;
        int left=start,right=segment.size()-start-count;
        int fragments=(left>0?1:0)+(right>0?1:0),maxRemaining=Math.max(left,right);
        String score="row="+first.rowNo()+",centerDistance="+Math.abs(candidateCenter-rowCenter)+",fragments="+fragments+",maxRemaining="+maxRemaining;
        return new SeatCandidate(first.rowNo(),first.rowLabel(),first.seatNo(),last.seatNo(),Math.abs(candidateCenter-rowCenter),fragments,maxRemaining,
                window.stream().map(SeatPosition::inventoryId).toList(),window.stream().map(SeatPosition::stadiumSeatId).toList(),
                window.stream().map(SeatPosition::seatNo).toList(),window.stream().map(SeatPosition::seatLabel).toList(),score);
    }
}
