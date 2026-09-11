package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.MatchTicketZoneMapper;
import com.example.leagueticket.service.MatchSeatInventoryService;
import com.example.leagueticket.service.StandardTicketZoneSelector;
import com.example.leagueticket.service.TicketSalePolicy;
import com.example.leagueticket.vo.TicketZoneAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class StandardTicketZoneSelectorImpl implements StandardTicketZoneSelector {
    private static final Set<String> TYPES=Set.of("VIP","NORMAL");
    private static final Map<String,Integer> DIRECTION_PRIORITY=Map.of("EAST",0,"WEST",1,"SOUTH",2,"NORTH",3);
    private final MatchTicketZoneMapper zoneMapper;
    private final MatchInfoMapper matchMapper;
    private final MatchSeatInventoryService inventoryService;
    private final TicketSalePolicy ticketSalePolicy;

    @Override
    public MatchTicketZone select(Long matchId,String ticketType,int ticketCount,boolean lockZones){
        if(matchId==null||ticketType==null||ticketCount<1)throw new BusinessException("matchId, ticketType and ticketCount are required");
        String type=ticketType.trim().toUpperCase(Locale.ROOT);
        if(!TYPES.contains(type))throw new BusinessException("ticketType must be VIP or NORMAL");
        MatchInfo match=matchMapper.findById(matchId);
        if(match==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");
        List<MatchTicketZone> zones=lockZones?zoneMapper.findByMatchForUpdate(matchId):zoneMapper.findByMatch(matchId);
        return zones.stream()
                .filter(zone->type.equals(zone.getTicketType()))
                .filter(zone->canCarry(match,zone,ticketCount))
                .min(Comparator.comparingInt(zone->DIRECTION_PRIORITY.getOrDefault(zone.getZoneDirection(),99)))
                .orElseThrow(()->new BusinessException(HttpStatus.CONFLICT,"没有任何单一实际票区能够承载本次购票数量"));
    }

    private boolean canCarry(MatchInfo match,MatchTicketZone zone,int ticketCount){
        TicketZoneAvailabilityResponse availability=inventoryService.availability(zone.getMatchZoneId());
        if(!ticketSalePolicy.evaluateSaleAvailability(match,zone,availability.availableSeatCount()).available())return false;
        return availability.availableSeatCount()>=ticketCount&&availability.maxContinuousCount()>=ticketCount;
    }
}
