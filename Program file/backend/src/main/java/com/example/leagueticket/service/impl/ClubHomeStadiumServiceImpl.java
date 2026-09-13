package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.ClubHomeStadiumService;
import com.example.leagueticket.vo.ClubProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class ClubHomeStadiumServiceImpl implements ClubHomeStadiumService {
    public static final String INCOMPLETE_MESSAGE="请先完善主场场馆及票务信息后再报名";
    private static final List<ZoneDefinition> ZONES=List.of(
            new ZoneDefinition("EAST","VIP","东 VIP",1,true),new ZoneDefinition("EAST","NORMAL","东 普通",2,true),
            new ZoneDefinition("WEST","VIP","西 VIP",3,true),new ZoneDefinition("WEST","NORMAL","西 普通",4,true),
            new ZoneDefinition("SOUTH","VIP","南 VIP",5,false),new ZoneDefinition("SOUTH","NORMAL","南 普通",6,false),
            new ZoneDefinition("NORTH","VIP","北 VIP",7,false),new ZoneDefinition("NORTH","NORMAL","北 普通",8,false));

    private final ClubInfoMapper clubMapper;
    private final StadiumInfoMapper stadiumMapper;
    private final StadiumZoneMapper zoneMapper;
    private final StadiumSeatMapper seatMapper;
    private final ClubHomeStadiumConfigMapper configMapper;

    @Override public ClubProfileResponse profile(Long clubId){
        ClubInfo club=requiredClub(clubId);StadiumInfo stadium=club.getHomeStadiumId()==null?null:stadiumMapper.findById(club.getHomeStadiumId());
        ClubHomeStadiumConfig config=configMapper.findByClubId(clubId);
        return ClubProfileResponse.from(club,stadium,config,isComplete(club,stadium,config));
    }

    @Override @Transactional
    public ClubProfileResponse updateProfile(Long clubId,ClubProfileRequest request){
        ClubInfo club=clubMapper.findByIdForUpdate(clubId);if(club==null)throw new BusinessException(HttpStatus.NOT_FOUND,"club not found");
        if(clubMapper.countByName(request.clubName().trim(),clubId)>0)throw new BusinessException(HttpStatus.CONFLICT,"club name already exists");
        HomeStadiumRequest home=request.homeStadium();if(home.vipPrice().compareTo(home.normalPrice())<=0)throw new BusinessException("VIP默认票价必须高于普通票价");ClubHomeStadiumConfig config=configMapper.findByClubId(clubId);StadiumInfo stadium;
        int capacity=capacity(home);
        if(config==null){
            if(stadiumMapper.countDuplicate(home.stadiumName().trim(),home.city().trim(),null)>0)throw new BusinessException(HttpStatus.CONFLICT,"同城市已存在同名场馆");
            stadium=new StadiumInfo();stadium.setStadiumName(home.stadiumName().trim());stadium.setCity(home.city().trim());stadium.setAddress(home.address().trim());
            stadium.setCapacity(capacity);stadium.setLayoutDesc(layout(home));stadium.setVenueModel("STANDARD_8");stadiumMapper.insert(stadium);
            generateLayout(stadium,home);
            config=new ClubHomeStadiumConfig();config.setClubId(clubId);config.setStadiumId(stadium.getStadiumId());copyConfig(config,home);configMapper.insert(config);
        }else{
            if(!Objects.equals(config.getRowsPerZone(),home.rowsPerZone())||!Objects.equals(config.getLongSideSeatsPerRow(),home.longSideSeatsPerRow())||!Objects.equals(config.getShortSideSeatsPerRow(),home.shortSideSeatsPerRow()))
                throw new BusinessException(HttpStatus.CONFLICT,"已生成的标准主场不允许直接重建座位结构");
            stadium=stadiumMapper.findById(config.getStadiumId());if(stadium==null||!"STANDARD_8".equals(stadium.getVenueModel()))throw new BusinessException(HttpStatus.CONFLICT,INCOMPLETE_MESSAGE);
            if(stadiumMapper.countDuplicate(home.stadiumName().trim(),home.city().trim(),stadium.getStadiumId())>0)throw new BusinessException(HttpStatus.CONFLICT,"同城市已存在同名场馆");
            stadium.setStadiumName(home.stadiumName().trim());stadium.setCity(home.city().trim());stadium.setAddress(home.address().trim());stadium.setCapacity(capacity);stadium.setLayoutDesc(layout(home));stadiumMapper.update(stadium);
            copyConfig(config,home);configMapper.updatePrices(config);
        }
        club.setClubName(request.clubName().trim());club.setShortName(trim(request.shortName()));club.setDescription(trim(request.description()));
        club.setHomeCity(home.city().trim());club.setHomeAddress(home.address().trim());club.setHomeStadiumId(stadium.getStadiumId());clubMapper.update(club);
        return profile(clubId);
    }

    @Override public ClubHomeStadiumConfig requireComplete(Long clubId){
        ClubInfo club=requiredClub(clubId);ClubHomeStadiumConfig config=configMapper.findByClubId(clubId);
        StadiumInfo stadium=config==null?null:stadiumMapper.findById(config.getStadiumId());
        if(!isComplete(club,stadium,config))throw new BusinessException(HttpStatus.CONFLICT,INCOMPLETE_MESSAGE);return config;
    }

    private boolean isComplete(ClubInfo club,StadiumInfo stadium,ClubHomeStadiumConfig config){
        if(config==null||stadium==null||!"STANDARD_8".equals(stadium.getVenueModel())||!Objects.equals(club.getHomeStadiumId(),config.getStadiumId()))return false;
        if(stadium.getCity()==null||stadium.getCity().isBlank()||stadium.getStadiumName()==null||stadium.getStadiumName().isBlank()||stadium.getAddress()==null||stadium.getAddress().isBlank())return false;
        List<StadiumZone> zones=zoneMapper.findByStadium(stadium.getStadiumId());if(zones.size()!=8)return false;
        Map<String,StadiumZone> actual=new HashMap<>();for(StadiumZone zone:zones){if(!"ACTIVE".equals(zone.getZoneStatus()))return false;actual.put(zone.getZoneDirection()+"_"+zone.getTicketType(),zone);}
        long seats=0;for(ZoneDefinition definition:ZONES){StadiumZone zone=actual.get(definition.code());if(zone==null)return false;List<StadiumSeat> values=seatMapper.findByZone(zone.getStadiumZoneId());int perRow=definition.longSide()?config.getLongSideSeatsPerRow():config.getShortSideSeatsPerRow();if(values.size()!=config.getRowsPerZone()*perRow)return false;
            Map<Integer,Long> rows=new HashMap<>();for(StadiumSeat seat:values){if(!"ACTIVE".equals(seat.getSeatStatus()))return false;rows.merge(seat.getRowNo(),1L,Long::sum);}int firstRow="NORMAL".equals(definition.type())?config.getRowsPerZone()+1:1;if(rows.size()!=config.getRowsPerZone()||rows.values().stream().anyMatch(v->v!=perRow)||!rows.keySet().equals(expectedRows(firstRow,config.getRowsPerZone())))return false;seats+=values.size();}
        return seats==stadium.getCapacity()&&stadium.getCapacity()==capacity(config);
    }
    private void generateLayout(StadiumInfo stadium,HomeStadiumRequest home){for(ZoneDefinition d:ZONES){StadiumZone zone=new StadiumZone();zone.setStadiumId(stadium.getStadiumId());zone.setZoneCode(d.code());zone.setZoneName(d.name());zone.setZoneDirection(d.direction());zone.setTicketType(d.type());zone.setSortNo(d.order());zone.setDescription("标准私有主场票区");zoneMapper.insert(zone);int count=d.longSide()?home.longSideSeatsPerRow():home.shortSideSeatsPerRow();int firstRow="NORMAL".equals(d.type())?home.rowsPerZone()+1:1;for(int row=firstRow;row<firstRow+home.rowsPerZone();row++){for(int seat=1;seat<=count;seat++){StadiumSeat value=new StadiumSeat();value.setStadiumId(stadium.getStadiumId());value.setStadiumZoneId(zone.getStadiumZoneId());value.setRowNo(row);value.setRowLabel(row+"排");value.setSeatNo(seat);value.setSeatLabel(seat+"座");value.setCenterDistance(BigDecimal.valueOf(Math.abs(seat-(count+1)/2.0)));seatMapper.insert(value);}}}}
    private Set<Integer> expectedRows(int firstRow,int count){Set<Integer> result=new HashSet<>();for(int row=firstRow;row<firstRow+count;row++)result.add(row);return result;}
    private int capacity(HomeStadiumRequest h){long v=(long)(h.longSideSeatsPerRow()+h.shortSideSeatsPerRow())*2*h.rowsPerZone()*2;if(v>Integer.MAX_VALUE)throw new BusinessException("场馆容量过大");return (int)v;}
    private int capacity(ClubHomeStadiumConfig c){long v=(long)(c.getLongSideSeatsPerRow()+c.getShortSideSeatsPerRow())*2*c.getRowsPerZone()*2;return v>Integer.MAX_VALUE?-1:(int)v;}
    private String layout(HomeStadiumRequest h){return "STANDARD_8: rows="+h.rowsPerZone()+", long="+h.longSideSeatsPerRow()+", short="+h.shortSideSeatsPerRow();}
    private void copyConfig(ClubHomeStadiumConfig c,HomeStadiumRequest h){c.setRowsPerZone(h.rowsPerZone());c.setLongSideSeatsPerRow(h.longSideSeatsPerRow());c.setShortSideSeatsPerRow(h.shortSideSeatsPerRow());c.setVipPrice(h.vipPrice());c.setNormalPrice(h.normalPrice());}
    private ClubInfo requiredClub(Long id){ClubInfo c=clubMapper.findById(id);if(c==null)throw new BusinessException(HttpStatus.NOT_FOUND,"club not found");return c;}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private record ZoneDefinition(String direction,String type,String name,int order,boolean longSide){String code(){return direction+"_"+type;}}
}
