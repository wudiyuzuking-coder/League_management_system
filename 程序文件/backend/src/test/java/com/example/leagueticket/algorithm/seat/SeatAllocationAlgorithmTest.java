package com.example.leagueticket.algorithm.seat;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class SeatAllocationAlgorithmTest {
    private final SeatAllocationAlgorithm algorithm=new SeatAllocationAlgorithm();

    @Test void oneSeatChoosesFrontRowThenPhysicalCenter(){
        var result=algorithm.evaluate(row(1,1,5,"AVAILABLE"),1);
        assertThat(result.best().rowNo()).isEqualTo(1);assertThat(result.best().seatNos()).containsExactly(3);
    }

    @Test void twoSeatsGenerateEverySlidingWindowAndChooseCenter(){
        var result=algorithm.evaluate(row(1,1,4,"AVAILABLE"),2);
        assertThat(result.candidates()).extracting(SeatCandidate::seatNos).containsExactly(List.of(2,3),List.of(1,2),List.of(3,4));
        assertThat(result.best().seatNos()).containsExactly(2,3);
    }

    @Test void threeAndFourSeatsFollowCenterAndStableStartTieBreak(){
        assertThat(algorithm.evaluate(row(1,1,8,"AVAILABLE"),3).best().seatNos()).containsExactly(3,4,5);
        assertThat(algorithm.evaluate(row(1,1,8,"AVAILABLE"),4).best().seatNos()).containsExactly(3,4,5,6);
    }

    @Test void neverCombinesRowsOrCrossesMissingSeat(){
        List<SeatPosition> twoRows=new ArrayList<>();twoRows.addAll(row(1,1,2,"AVAILABLE"));twoRows.addAll(row(2,1,2,"AVAILABLE"));
        var cross=algorithm.evaluate(twoRows,4);assertThat(cross.best()).isNull();assertThat(cross.maxContinuousCount()).isEqualTo(2);
        List<SeatPosition> gap=new ArrayList<>(row(1,1,2,"AVAILABLE"));gap.addAll(row(1,4,5,"AVAILABLE"));
        assertThat(algorithm.evaluate(gap,4).best()).isNull();
    }

    @Test void disabledLockedAndSoldAreExcludedNaturally(){
        List<SeatPosition> seats=new ArrayList<>();seats.add(seat(1,1,"AVAILABLE",1,5));seats.add(seat(1,2,"DISABLED",1,5));seats.add(seat(1,3,"LOCKED",1,5));seats.add(seat(1,4,"SOLD",1,5));seats.add(seat(1,5,"AVAILABLE",1,5));
        var result=algorithm.evaluate(seats,2);assertThat(result.best()).isNull();assertThat(result.maxContinuousCount()).isEqualTo(1);
    }

    @Test void frontRowAlwaysWinsBeforeCenterQuality(){
        List<SeatPosition> seats=new ArrayList<>();for(int i=1;i<=4;i++)seats.add(seat(1,i,"AVAILABLE",1,20));for(int i=8;i<=11;i++)seats.add(seat(2,i,"AVAILABLE",1,20));
        assertThat(algorithm.evaluate(seats,4).best().rowNo()).isEqualTo(1);
    }

    @Test void physicalRowCenterDoesNotShrinkWithCurrentAvailability(){
        List<SeatPosition> seats=List.of(seat(1,1,"AVAILABLE",1,10),seat(1,2,"AVAILABLE",1,10),seat(1,8,"AVAILABLE",1,10),seat(1,9,"AVAILABLE",1,10));
        assertThat(algorithm.evaluate(seats,2).best().seatNos()).containsExactly(8,9);
    }

    @Test void comparatorUsesFragmentThenLargestRemainingSpaceThenStartSeat(){
        SeatCandidate fewer=candidate(0.5,1,2,3),more=candidate(0.5,2,9,3);
        assertThat(SeatAllocationAlgorithm.CANDIDATE_ORDER.compare(fewer,more)).isNegative();
        SeatCandidate larger=candidate(0.5,1,6,4),smaller=candidate(0.5,1,3,2);
        assertThat(SeatAllocationAlgorithm.CANDIDATE_ORDER.compare(larger,smaller)).isNegative();
        SeatCandidate left=candidate(0.5,1,3,1),right=candidate(0.5,1,3,5);
        assertThat(SeatAllocationAlgorithm.CANDIDATE_ORDER.compare(left,right)).isNegative();
    }

    private List<SeatPosition> row(int row,int from,int to,String status){List<SeatPosition> seats=new ArrayList<>();for(int i=from;i<=to;i++)seats.add(seat(row,i,status,from,to));return seats;}
    private SeatPosition seat(int row,int no,String status,int min,int max){long id=row*100L+no;return new SeatPosition(id,id,row,row+"排",no,no+"座",status,min,max,0);}
    private SeatCandidate candidate(double distance,int fragments,int maxRemaining,int start){return new SeatCandidate(1,"1排",start,start+1,distance,fragments,maxRemaining,List.of(),List.of(),List.of(),List.of(),"");}
}
