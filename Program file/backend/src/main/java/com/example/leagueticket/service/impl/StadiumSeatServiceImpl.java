package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.StadiumSeatMapper;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.SeatRowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class StadiumSeatServiceImpl implements StadiumSeatService {
    private final StadiumSeatMapper mapper;private final StadiumZoneService zoneService;
    public List<StadiumSeat> list(Long zoneId){zoneService.getById(zoneId);return mapper.findByZone(zoneId);}
    public List<SeatRowResponse> layout(Long zoneId){Map<Integer,List<StadiumSeat>> grouped=new LinkedHashMap<>();for(StadiumSeat s:list(zoneId))grouped.computeIfAbsent(s.getRowNo(),key->new ArrayList<>()).add(s);List<SeatRowResponse> result=new ArrayList<>();grouped.forEach((row,seats)->result.add(new SeatRowResponse(row,seats.get(0).getRowLabel(),seats)));return result;}
    @Transactional public StadiumSeat create(Long zoneId,StadiumSeatRequest r){StadiumZone zone=zoneService.getById(zoneId);validateConflict(zoneId,r,null);StadiumSeat seat=copy(new StadiumSeat(),r);seat.setStadiumId(zone.getStadiumId());seat.setStadiumZoneId(zoneId);seat.setCenterDistance(BigDecimal.ZERO);mapper.insert(seat);return mapper.findById(seat.getStadiumSeatId());}
    @Transactional public StadiumSeat update(Long id,StadiumSeatRequest r){StadiumSeat seat=getById(id);validateConflict(seat.getStadiumZoneId(),r,id);mapper.update(copy(seat,r));return getById(id);}
    @Transactional public StadiumSeat updateStatus(Long id,String status){getById(id);if(!Set.of("ACTIVE","DISABLED").contains(status))throw new BusinessException("invalid physical seat status");mapper.updateStatus(id,status);return getById(id);}
    @Transactional public int batchCreate(Long zoneId,SeatBatchRequest request){StadiumZone zone=zoneService.getById(zoneId);Set<String> seq=new HashSet<>(),labels=new HashSet<>();List<StadiumSeat> pending=new ArrayList<>();for(var row:request.rows()){double middle=row.startSeatNo()+(row.seatCount()-1)/2.0;for(int i=0;i<row.seatCount();i++){int seatNo=row.startSeatNo()+i;String seatLabel=String.valueOf(seatNo);String seqKey=row.rowNo()+":"+seatNo,labelKey=row.rowLabel().trim()+":"+seatLabel;if(!seq.add(seqKey)||!labels.add(labelKey)||mapper.countConflict(zoneId,row.rowNo(),seatNo,row.rowLabel().trim(),seatLabel,null)>0)throw new BusinessException(HttpStatus.CONFLICT,"batch seat generation conflicts with an existing or duplicate seat");StadiumSeat seat=new StadiumSeat();seat.setStadiumId(zone.getStadiumId());seat.setStadiumZoneId(zoneId);seat.setRowNo(row.rowNo());seat.setRowLabel(row.rowLabel().trim());seat.setSeatNo(seatNo);seat.setSeatLabel(seatLabel);seat.setCenterDistance(BigDecimal.valueOf(Math.abs(seatNo-middle)));pending.add(seat);}}pending.forEach(mapper::insert);return pending.size();}
    private StadiumSeat getById(Long id){StadiumSeat s=mapper.findById(id);if(s==null)throw new BusinessException(HttpStatus.NOT_FOUND,"stadium seat not found");return s;}
    private void validateConflict(Long zoneId,StadiumSeatRequest r,Long id){if(mapper.countConflict(zoneId,r.rowNo(),r.seatNo(),r.rowLabel().trim(),r.seatLabel().trim(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"seat position or label already exists in this zone");}
    private StadiumSeat copy(StadiumSeat s,StadiumSeatRequest r){s.setRowNo(r.rowNo());s.setRowLabel(r.rowLabel().trim());s.setSeatNo(r.seatNo());s.setSeatLabel(r.seatLabel().trim());return s;}
}
