package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.StadiumInfo;
import com.example.leagueticket.dto.StadiumRequest;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.StadiumInfoMapper;
import com.example.leagueticket.service.StadiumInfoService;
import com.example.leagueticket.mapper.StadiumSeatMapper;
import com.example.leagueticket.vo.StadiumCapacityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("dev") @RequiredArgsConstructor
public class StadiumInfoServiceImpl implements StadiumInfoService {
    private final StadiumInfoMapper mapper;
    private final StadiumSeatMapper seatMapper;
    public List<StadiumInfo> list(){return mapper.findAll();}
    public List<StadiumInfo> search(String name,String city){return mapper.search(name,city);}
    public StadiumInfo getById(Long id){StadiumInfo value=mapper.findById(id);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"stadium not found");return value;}
    @Transactional public StadiumInfo create(StadiumRequest request){validate(request,null);StadiumInfo s=copy(new StadiumInfo(),request);mapper.insert(s);return getById(s.getStadiumId());}
    @Transactional public StadiumInfo update(Long id,StadiumRequest request){StadiumInfo s=getById(id);validate(request,id);mapper.update(copy(s,request));return getById(id);}
    @Transactional public StadiumInfo updateStatus(Long id,String status){getById(id);if(!Set.of("ACTIVE","DISABLED").contains(status))throw new BusinessException("invalid stadium status");mapper.updateStatus(id,status);return getById(id);}
    public StadiumCapacityResponse capacitySummary(Long id){StadiumInfo s=getById(id);return new StadiumCapacityResponse(s.getCapacity(),seatMapper.countTotal(id),seatMapper.countStatus(id,"ACTIVE"),seatMapper.countStatus(id,"DISABLED"));}
    private void validate(StadiumRequest r,Long id){if(mapper.countDuplicate(r.stadiumName().trim(),r.city().trim(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"stadium name already exists in this city");}
    private StadiumInfo copy(StadiumInfo s,StadiumRequest r){s.setStadiumName(r.stadiumName().trim());s.setCity(r.city().trim());s.setAddress(r.address().trim());s.setCapacity(r.capacity());s.setLayoutDesc(r.layoutDesc()==null||r.layoutDesc().isBlank()?null:r.layoutDesc().trim());return s;}
}
