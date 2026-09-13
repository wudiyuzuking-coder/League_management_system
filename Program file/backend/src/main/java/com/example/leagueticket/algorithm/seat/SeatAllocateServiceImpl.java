package com.example.leagueticket.algorithm.seat;

import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDateTime;

@Service @Profile("dev") @RequiredArgsConstructor
public class SeatAllocateServiceImpl implements SeatAllocateService {
    private final MatchTicketZoneMapper zoneMapper;
    private final MatchSeatInventoryMapper inventoryMapper;
    private final SystemConfigMapper configMapper;
    private final SeatAllocationAlgorithm algorithm;

    public SeatAllocationResponse preview(Long matchZoneId,int ticketCount){
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=bestAvailable(context.positions,context.evaluation,ticketCount);
        if(best==null)throw noSolution(ticketCount,context.evaluation.maxContinuousCount());
        return SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best,context.zone.getZoneNameSnapshot());
    }

    public SeatAllocationDebugResponse debug(Long matchZoneId,int ticketCount){
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=bestAvailable(context.positions,context.evaluation,ticketCount);
        return new SeatAllocationDebugResponse(best==null?null:SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best,context.zone.getZoneNameSnapshot()),context.evaluation.maxContinuousCount(),context.evaluation.candidates());
    }

    @Transactional
    public SeatAllocationResponse selectAndClaimAvailable(Long matchZoneId,int ticketCount){
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=bestAvailable(context.positions,context.evaluation,ticketCount);
        if(best==null)throw noSolution(ticketCount,context.evaluation.maxContinuousCount());
        Map<Long,Integer> versions=new HashMap<>();context.positions.forEach(s->versions.put(s.inventoryId(),s.version()));
        for(Long inventoryId:best.inventoryIds())if(inventoryMapper.claimAvailableForTest(inventoryId,versions.get(inventoryId))!=1)
            throw new BusinessException(HttpStatus.CONFLICT,"seat inventory changed concurrently; please recalculate");
        return SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best,context.zone.getZoneNameSnapshot());
    }

    @Transactional
    public SeatAllocationResponse selectAndLockSeats(Long matchZoneId,int ticketCount,Long orderId,
                                                     LocalDateTime lockedAt,LocalDateTime expireTime){
        validateCount(ticketCount);
        MatchTicketZone zone=zoneMapper.findById(matchZoneId);
        if(zone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match ticket zone not found");
        List<MatchSeatInventory> rows=inventoryMapper.findForAllocationForUpdate(matchZoneId);
        List<SeatPosition> positions=rows.stream().map(this::position).toList();
        SeatAllocationEvaluation evaluation=algorithm.evaluate(positions,ticketCount);
        SeatCandidate best=bestAvailable(positions,evaluation,ticketCount);
        if(best==null)throw noSolution(ticketCount,evaluation.maxContinuousCount());
        Map<Long,Integer> versions=new HashMap<>();positions.forEach(s->versions.put(s.inventoryId(),s.version()));
        for(Long inventoryId:best.inventoryIds()){
            int changed=inventoryMapper.lockAvailable(inventoryId,versions.get(inventoryId),orderId,lockedAt,expireTime);
            if(changed!=1)throw new BusinessException(HttpStatus.CONFLICT,"seat inventory changed concurrently; please submit again");
        }
        return SeatAllocationResponse.from(matchZoneId,zone.getMatchId(),ticketCount,best,zone.getZoneNameSnapshot());
    }

    private Context evaluate(Long matchZoneId,int ticketCount){
        validateCount(ticketCount);
        MatchTicketZone zone=zoneMapper.findById(matchZoneId);if(zone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match ticket zone not found");
        List<SeatPosition> positions=inventoryMapper.findForAllocation(matchZoneId).stream().map(this::position).toList();
        return new Context(zone,positions,algorithm.evaluate(positions,ticketCount));
    }

    private SeatPosition position(MatchSeatInventory s){return new SeatPosition(s.getInventoryId(),s.getStadiumSeatId(),s.getRowNo(),s.getRowLabel(),s.getSeatNo(),s.getSeatLabel(),s.getInventoryStatus(),s.getPhysicalMinSeatNo(),s.getPhysicalMaxSeatNo(),s.getVersion());}
    /**
     * Keep SeatAllocationAlgorithm as the sole contiguous-seat scorer.  When no
     * full run exists but the zone has enough stock, retain its longest run and
     * fill the remaining seats in the normal front-row / centre-first order.
     */
    private SeatCandidate bestAvailable(List<SeatPosition> positions,SeatAllocationEvaluation evaluation,int ticketCount){
        if(evaluation.best()!=null)return evaluation.best();
        List<SeatPosition> available=positions.stream().filter(s->"AVAILABLE".equals(s.inventoryStatus())).toList();
        if(available.size()<ticketCount)return null;
        List<SeatPosition> longest=longestSegment(available);
        List<SeatPosition> selected=new ArrayList<>(longest);
        Comparator<SeatPosition> preferred=Comparator.comparingInt(SeatPosition::rowNo)
                .thenComparingDouble(this::centerDistance).thenComparingInt(SeatPosition::seatNo);
        available.stream().filter(s->!selected.contains(s)).sorted(preferred)
                .limit(ticketCount-selected.size()).forEach(selected::add);
        selected.sort(Comparator.comparingInt(SeatPosition::rowNo).thenComparingInt(SeatPosition::seatNo));
        SeatPosition first=selected.get(0),last=selected.get(selected.size()-1);
        boolean sameRow=selected.stream().allMatch(s->s.rowNo()==first.rowNo());
        String rowLabel=sameRow?first.rowLabel()+"（尽量连坐）":"多排（尽量连坐）";
        return new SeatCandidate(first.rowNo(),rowLabel,first.seatNo(),last.seatNo(),0,0,0,
                selected.stream().map(SeatPosition::inventoryId).toList(),selected.stream().map(SeatPosition::stadiumSeatId).toList(),
                selected.stream().map(SeatPosition::seatNo).toList(),selected.stream().map(s->s.rowLabel()+s.seatLabel()).toList(),
                "best-effort-contiguous; maxContinuous="+evaluation.maxContinuousCount());
    }
    private List<SeatPosition> longestSegment(List<SeatPosition> available){
        Map<Integer,List<SeatPosition>> byRow=new TreeMap<>();
        available.stream().sorted(Comparator.comparingInt(SeatPosition::rowNo).thenComparingInt(SeatPosition::seatNo))
                .forEach(s->byRow.computeIfAbsent(s.rowNo(),ignored->new ArrayList<>()).add(s));
        List<SeatPosition> best=List.of();
        for(List<SeatPosition> row:byRow.values()){
            List<SeatPosition> run=new ArrayList<>();Integer previous=null;
            for(SeatPosition seat:row){
                if(previous!=null&&seat.seatNo()!=previous+1){best=preferRun(best,run);run=new ArrayList<>();}
                run.add(seat);previous=seat.seatNo();
            }
            best=preferRun(best,run);
        }
        return best;
    }
    private List<SeatPosition> preferRun(List<SeatPosition> current,List<SeatPosition> candidate){
        if(candidate.size()>current.size())return candidate;
        if(candidate.size()<current.size()||candidate.isEmpty())return current;
        SeatPosition a=current.get(0),b=candidate.get(0);
        if(b.rowNo()<a.rowNo())return candidate;
        if(b.rowNo()>a.rowNo())return current;
        return centerDistance(b)<centerDistance(a)?candidate:current;
    }
    private double centerDistance(SeatPosition seat){return Math.abs(seat.seatNo()-(seat.physicalMinSeatNo()+seat.physicalMaxSeatNo())/2.0);}
    private void validateCount(int ticketCount){int max=maxTickets();if(ticketCount<1||ticketCount>max)throw new BusinessException("ticketCount must be between 1 and "+max);}
    private int maxTickets(){String value=configMapper.findEnabledValue("MAX_TICKETS_PER_ORDER");try{return value==null?4:Integer.parseInt(value);}catch(NumberFormatException ignored){return 4;}}
    private BusinessException noSolution(int requested,int max){return new BusinessException(HttpStatus.CONFLICT,"cannot satisfy "+requested+" consecutive seats; current maxContinuousCount="+max);}
    private record Context(MatchTicketZone zone,List<SeatPosition> positions,SeatAllocationEvaluation evaluation){}
}
