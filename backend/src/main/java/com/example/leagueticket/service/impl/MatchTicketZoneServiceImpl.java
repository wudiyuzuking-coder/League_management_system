package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.MatchTicketZoneRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class MatchTicketZoneServiceImpl implements MatchTicketZoneService {
    private static final Map<String,Set<String>> NEXT=Map.of(
            "DRAFT",Set.of("ON_SALE","CLOSED"),"ON_SALE",Set.of("PAUSED","CLOSED"),
            "PAUSED",Set.of("ON_SALE","CLOSED"),"CLOSED",Set.of());
    private final MatchTicketZoneMapper mapper;
    private final MatchSeatInventoryMapper inventoryMapper;
    private final MatchInfoService matchService;
    private final StadiumZoneService stadiumZoneService;
    private final MatchSeatInventoryService inventoryService;
    private final SystemConfigMapper configMapper;
    private final SystemTimeService systemTimeService;

    public List<MatchTicketZoneResponse> list(Long matchId){MatchInfo match=matchService.getById(matchId);return mapper.findByMatch(matchId).stream().map(z->response(z,match)).toList();}
    public MatchTicketZoneResponse detail(Long id){MatchTicketZone zone=getEntity(id);return response(zone,matchService.getById(zone.getMatchId()));}
    public MatchTicketZone getEntity(Long id){MatchTicketZone zone=mapper.findById(id);if(zone==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match ticket zone not found");return zone;}

    @Transactional
    public MatchTicketZone create(Long matchId,Long creatorId,MatchTicketZoneRequest request){
        MatchInfo match=matchService.getById(matchId);validateMatchConfigurable(match);StadiumZone staticZone=validateRequest(match,request,null);
        MatchTicketZone zone=new MatchTicketZone();zone.setMatchId(matchId);zone.setStadiumZoneId(request.stadiumZoneId());zone.setCreatedBy(creatorId);
        copy(zone,request,staticZone);mapper.insert(zone);return getEntity(zone.getMatchZoneId());
    }

    @Transactional
    public MatchTicketZone update(Long id,MatchTicketZoneRequest request){
        MatchTicketZone zone=getEntity(id);if(!"DRAFT".equals(zone.getZoneStatus()))throw new BusinessException("only a DRAFT ticket zone can be edited");
        MatchInfo match=matchService.getById(zone.getMatchId());validateMatchConfigurable(match);
        if(!zone.getStadiumZoneId().equals(request.stadiumZoneId())&&inventoryMapper.countTotal(id)>0)throw new BusinessException("stadium zone cannot be changed after inventory generation");
        StadiumZone staticZone=validateRequest(match,request,id);copy(zone,request,staticZone);mapper.update(zone);return getEntity(id);
    }

    @Transactional
    public MatchTicketZone updateStatus(Long id,String status){
        MatchTicketZone zone=getEntity(id);
        if(!NEXT.containsKey(status))throw new BusinessException("invalid ticket zone status");
        if(zone.getZoneStatus().equals(status))return zone;
        if(!NEXT.get(zone.getZoneStatus()).contains(status))throw new BusinessException("invalid ticket zone status transition: "+zone.getZoneStatus()+" -> "+status);
        if("ON_SALE".equals(status))validateOnSale(zone);
        mapper.updateStatus(id,status);return getEntity(id);
    }

    private void validateOnSale(MatchTicketZone zone){
        MatchInfo match=matchService.getById(zone.getMatchId());
        if(!"PUBLISHED".equals(match.getMatchStatus()))throw new BusinessException("only a PUBLISHED match can sell tickets");
        LocalDateTime now=systemTimeService.now();if(now.isBefore(zone.getSaleStartTime())||!now.isBefore(zone.getSaleEndTime()))throw new BusinessException("current time is outside the sale period");
        StadiumZone staticZone=stadiumZoneService.getById(zone.getStadiumZoneId());if(!"ACTIVE".equals(staticZone.getZoneStatus()))throw new BusinessException("disabled stadium zone cannot go on sale");
        if(inventoryMapper.countTotal(zone.getMatchZoneId())==0)throw new BusinessException("inventory must be generated explicitly before going on sale");
        if(inventoryMapper.countStatus(zone.getMatchZoneId(),"AVAILABLE")==0)throw new BusinessException("at least one AVAILABLE seat is required before going on sale");
    }

    private StadiumZone validateRequest(MatchInfo match,MatchTicketZoneRequest request,Long excludeId){
        StadiumZone zone=stadiumZoneService.getById(request.stadiumZoneId());
        if(!"ACTIVE".equals(zone.getZoneStatus()))throw new BusinessException("disabled stadium zone cannot be configured");
        if(!zone.getStadiumId().equals(match.getStadiumId()))throw new BusinessException("stadium zone does not belong to the match stadium");
        if(mapper.countDuplicate(match.getMatchId(),request.stadiumZoneId(),excludeId)>0)throw new BusinessException(HttpStatus.CONFLICT,"this stadium zone is already configured for the match");
        if(!request.saleEndTime().isAfter(request.saleStartTime()))throw new BusinessException("saleEndTime must be after saleStartTime");
        int stopMinutes=saleStopMinutes();LocalDateTime latest=match.getMatchTime().minusMinutes(stopMinutes);
        if(request.saleEndTime().isAfter(latest))throw new BusinessException("saleEndTime must not be later than "+stopMinutes+" minutes before match start");
        return zone;
    }

    private int saleStopMinutes(){String value=configMapper.findEnabledValue("SALE_STOP_BEFORE_MINUTES");try{return value==null?30:Integer.parseInt(value);}catch(NumberFormatException ignored){return 30;}}
    private void validateMatchConfigurable(MatchInfo match){if(!Set.of("DRAFT","PUBLISHED").contains(match.getMatchStatus()))throw new BusinessException("ticket zones cannot be configured in current match status");}
    private void copy(MatchTicketZone zone,MatchTicketZoneRequest request,StadiumZone staticZone){zone.setStadiumZoneId(request.stadiumZoneId());zone.setZoneNameSnapshot(staticZone.getZoneName());zone.setTicketPrice(request.price());zone.setSaleStartTime(request.saleStartTime());zone.setSaleEndTime(request.saleEndTime());}
    private MatchTicketZoneResponse response(MatchTicketZone zone,MatchInfo match){TicketZoneAvailabilityResponse a=inventoryService.availability(zone.getMatchZoneId());LocalDateTime now=systemTimeService.now();boolean sale="ON_SALE".equals(zone.getZoneStatus())&&"PUBLISHED".equals(match.getMatchStatus())&&!now.isBefore(zone.getSaleStartTime())&&now.isBefore(zone.getSaleEndTime())&&a.availableSeatCount()>0;return new MatchTicketZoneResponse(zone.getMatchZoneId(),zone.getMatchId(),zone.getStadiumZoneId(),zone.getCreatedBy(),zone.getZoneNameSnapshot(),zone.getZoneCode(),zone.getTicketPrice(),zone.getZoneStatus(),zone.getSaleStartTime(),zone.getSaleEndTime(),a.totalSeatCount(),a.availableSeatCount(),a.lockedSeatCount(),a.soldSeatCount(),a.disabledSeatCount(),a.maxContinuousCount(),sale);}
}
