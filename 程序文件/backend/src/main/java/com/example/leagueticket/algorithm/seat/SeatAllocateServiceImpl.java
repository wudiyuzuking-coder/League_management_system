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
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=context.evaluation.best();
        if(best==null)throw noSolution(ticketCount,context.evaluation.maxContinuousCount());
        return SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best);
    }

    public SeatAllocationDebugResponse debug(Long matchZoneId,int ticketCount){
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=context.evaluation.best();
        return new SeatAllocationDebugResponse(best==null?null:SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best),context.evaluation.maxContinuousCount(),context.evaluation.candidates());
    }

    @Transactional
    public SeatAllocationResponse selectAndClaimAvailable(Long matchZoneId,int ticketCount){
        Context context=evaluate(matchZoneId,ticketCount);SeatCandidate best=context.evaluation.best();
        if(best==null)throw noSolution(ticketCount,context.evaluation.maxContinuousCount());
        Map<Long,Integer> versions=new HashMap<>();context.positions.forEach(s->versions.put(s.inventoryId(),s.version()));
        for(Long inventoryId:best.inventoryIds())if(inventoryMapper.claimAvailableForTest(inventoryId,versions.get(inventoryId))!=1)
            throw new BusinessException(HttpStatus.CONFLICT,"seat inventory changed concurrently; please recalculate");
        return SeatAllocationResponse.from(matchZoneId,context.zone.getMatchId(),ticketCount,best);
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
        SeatCandidate best=evaluation.best();
        if(best==null)throw noSolution(ticketCount,evaluation.maxContinuousCount());
        Map<Long,Integer> versions=new HashMap<>();positions.forEach(s->versions.put(s.inventoryId(),s.version()));
        for(Long inventoryId:best.inventoryIds()){
            int changed=inventoryMapper.lockAvailable(inventoryId,versions.get(inventoryId),orderId,lockedAt,expireTime);
            if(changed!=1)throw new BusinessException(HttpStatus.CONFLICT,"seat inventory changed concurrently; please submit again");
        }
        return SeatAllocationResponse.from(matchZoneId,zone.getMatchId(),ticketCount,best);
    }

    private Context evaluate(Long matchZoneId,int ticketCount){
        validateCount(ticketCount);
        MatchTicketZone zone=zoneMapper.findById(matchZoneId);if(zone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match ticket zone not found");
        List<SeatPosition> positions=inventoryMapper.findForAllocation(matchZoneId).stream().map(this::position).toList();
        return new Context(zone,positions,algorithm.evaluate(positions,ticketCount));
    }

    private SeatPosition position(MatchSeatInventory s){return new SeatPosition(s.getInventoryId(),s.getStadiumSeatId(),s.getRowNo(),s.getRowLabel(),s.getSeatNo(),s.getSeatLabel(),s.getInventoryStatus(),s.getPhysicalMinSeatNo(),s.getPhysicalMaxSeatNo(),s.getVersion());}
    private void validateCount(int ticketCount){int max=maxTickets();if(ticketCount<1||ticketCount>max)throw new BusinessException("ticketCount must be between 1 and "+max);}
    private int maxTickets(){String value=configMapper.findEnabledValue("MAX_TICKETS_PER_ORDER");try{return value==null?4:Integer.parseInt(value);}catch(NumberFormatException ignored){return 4;}}
    private BusinessException noSolution(int requested,int max){return new BusinessException(HttpStatus.CONFLICT,"cannot satisfy "+requested+" consecutive seats; current maxContinuousCount="+max);}
    private record Context(MatchTicketZone zone,List<SeatPosition> positions,SeatAllocationEvaluation evaluation){}
}
