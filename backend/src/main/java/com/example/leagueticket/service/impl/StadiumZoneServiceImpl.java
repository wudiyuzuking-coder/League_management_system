package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.StadiumZoneRequest;
import com.example.leagueticket.entity.StadiumZone;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.StadiumZoneMapper;
import com.example.leagueticket.service.StadiumInfoService;
import com.example.leagueticket.service.StadiumZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service @Profile("dev") @RequiredArgsConstructor
public class StadiumZoneServiceImpl implements StadiumZoneService {
    private final StadiumZoneMapper mapper;private final StadiumInfoService stadiumService;
    public List<StadiumZone> list(Long stadiumId){stadiumService.getById(stadiumId);return mapper.findByStadium(stadiumId);}
    public StadiumZone getById(Long id){StadiumZone z=mapper.findById(id);if(z==null)throw new BusinessException(HttpStatus.NOT_FOUND,"stadium zone not found");return z;}
    @Transactional public StadiumZone create(Long stadiumId,StadiumZoneRequest r){stadiumService.getById(stadiumId);validate(stadiumId,r,null);StadiumZone z=copy(new StadiumZone(),r);z.setStadiumId(stadiumId);mapper.insert(z);return getById(z.getStadiumZoneId());}
    @Transactional public StadiumZone update(Long id,StadiumZoneRequest r){StadiumZone z=getById(id);validate(z.getStadiumId(),r,id);mapper.update(copy(z,r));return getById(id);}
    @Transactional public StadiumZone updateStatus(Long id,String status){getById(id);if(!Set.of("ACTIVE","DISABLED").contains(status))throw new BusinessException("invalid stadium zone status");mapper.updateStatus(id,status);return getById(id);}
    private void validate(Long stadiumId,StadiumZoneRequest r,Long id){if(mapper.countCode(stadiumId,r.zoneCode().trim(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"zone code already exists in this stadium");if(mapper.countName(stadiumId,r.zoneName().trim(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"zone name already exists in this stadium");}
    private StadiumZone copy(StadiumZone z,StadiumZoneRequest r){z.setZoneCode(r.zoneCode().trim());z.setZoneName(r.zoneName().trim());z.setSortNo(r.sortNo());z.setDescription(r.description()==null||r.description().isBlank()?null:r.description().trim());return z;}
}
