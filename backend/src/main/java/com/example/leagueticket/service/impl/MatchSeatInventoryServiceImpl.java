package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.*;
import com.example.leagueticket.algorithm.seat.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.MatchSeatInventoryService;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class MatchSeatInventoryServiceImpl implements MatchSeatInventoryService {
    private final MatchTicketZoneMapper zoneMapper;
    private final MatchSeatInventoryMapper inventoryMapper;
    private final StadiumZoneMapper stadiumZoneMapper;
    private final MatchInfoMapper matchMapper;
    private final SeatAllocationAlgorithm allocationAlgorithm;

    @Transactional
    public int generate(Long matchZoneId){
        MatchTicketZone zone=requireZone(matchZoneId);
        if(!"DRAFT".equals(zone.getZoneStatus()))throw new BusinessException("inventory can only be generated for a DRAFT ticket zone");
        if(inventoryMapper.countTotal(matchZoneId)>0)throw new BusinessException(HttpStatus.CONFLICT,"inventory already exists; repeated generation is not allowed");
        MatchInfo match=matchMapper.findById(zone.getMatchId());
        if(match==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");
        StadiumZone staticZone=stadiumZoneMapper.findById(zone.getStadiumZoneId());
        if(staticZone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"stadium zone not found");
        if(!staticZone.getStadiumId().equals(match.getStadiumId()))throw new BusinessException("stadium zone does not belong to the match stadium");
        if(!"ACTIVE".equals(staticZone.getZoneStatus()))throw new BusinessException("disabled stadium zone cannot generate inventory");
        int created=inventoryMapper.generate(match.getMatchId(),matchZoneId,zone.getStadiumZoneId());
        if(created<=0)throw new BusinessException("no ACTIVE physical seat is available for inventory generation");
        return created;
    }

    @Transactional
    public MatchSeatInventory updateStatus(Long inventoryId,String status){
        if(!Set.of("AVAILABLE","DISABLED").contains(status))throw new BusinessException("only AVAILABLE or DISABLED is allowed in this stage");
        MatchSeatInventory inventory=inventoryMapper.findById(inventoryId);
        if(inventory==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match seat inventory not found");
        if(!Set.of("AVAILABLE","DISABLED").contains(inventory.getInventoryStatus()))throw new BusinessException("LOCKED or SOLD inventory cannot be managed by this endpoint");
        if(status.equals(inventory.getInventoryStatus()))return inventory;
        inventoryMapper.updateStatus(inventoryId,status);
        return inventoryMapper.findById(inventoryId);
    }

    public TicketZoneAvailabilityResponse availability(Long matchZoneId){
        requireZone(matchZoneId);
        List<SeatPosition> positions=inventoryMapper.findForAllocation(matchZoneId).stream().map(this::position).toList();
        long available=positions.stream().filter(s->"AVAILABLE".equals(s.inventoryStatus())).count();
        long locked=positions.stream().filter(s->"LOCKED".equals(s.inventoryStatus())).count();
        long sold=positions.stream().filter(s->"SOLD".equals(s.inventoryStatus())).count();
        long disabled=positions.stream().filter(s->"DISABLED".equals(s.inventoryStatus())).count();
        return new TicketZoneAvailabilityResponse(positions.size(),available,locked,sold,disabled,allocationAlgorithm.maxContinuous(positions));
    }

    public List<InventoryRowResponse> layout(Long matchZoneId){
        requireZone(matchZoneId);
        Map<Integer,List<MatchSeatInventory>> rows=new LinkedHashMap<>();
        for(MatchSeatInventory seat:inventoryMapper.findLayout(matchZoneId))rows.computeIfAbsent(seat.getRowNo(),ignored->new ArrayList<>()).add(seat);
        List<InventoryRowResponse> result=new ArrayList<>();
        rows.forEach((rowNo,seats)->result.add(new InventoryRowResponse(rowNo,seats.get(0).getRowLabel(),seats)));
        return result;
    }

    private SeatPosition position(MatchSeatInventory s){return new SeatPosition(s.getInventoryId(),s.getStadiumSeatId(),s.getRowNo(),s.getRowLabel(),s.getSeatNo(),s.getSeatLabel(),s.getInventoryStatus(),s.getPhysicalMinSeatNo(),s.getPhysicalMaxSeatNo(),s.getVersion());}

    private MatchTicketZone requireZone(Long id){MatchTicketZone zone=zoneMapper.findById(id);if(zone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match ticket zone not found");return zone;}
}
