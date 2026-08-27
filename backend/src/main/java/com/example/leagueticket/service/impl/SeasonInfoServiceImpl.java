package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.SeasonRequest;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.RoundInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.SeasonInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service @Profile("dev") @RequiredArgsConstructor
public class SeasonInfoServiceImpl implements SeasonInfoService {
    private static final Map<String,String> NEXT=Map.of("DRAFT","ACTIVE","ACTIVE","FINISHED");
    private final SeasonInfoMapper mapper;
    private final RoundInfoMapper roundMapper;
    public List<SeasonInfo> list(){return mapper.findAll();}
    public SeasonInfo getById(Long id){SeasonInfo value=mapper.findById(id);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");return value;}
    @Transactional public SeasonInfo create(SeasonRequest request){validate(request,null);SeasonInfo s=copy(new SeasonInfo(),request);s.setSeasonStatus("DRAFT");mapper.insert(s);return getById(s.getSeasonId());}
    @Transactional public SeasonInfo update(Long id,SeasonRequest request){SeasonInfo s=getById(id);validate(request,id);if(roundMapper.countOutsideRange(id,request.startDate(),request.endDate())>0)throw new BusinessException("season date range cannot exclude existing rounds");mapper.update(copy(s,request));return getById(id);}
    @Transactional public SeasonInfo updateStatus(Long id,String status){SeasonInfo s=getById(id);if(s.getSeasonStatus().equals(status))return s;String next=NEXT.get(s.getSeasonStatus());if(!status.equals(next))throw new BusinessException("invalid season status transition: "+s.getSeasonStatus()+" -> "+status);mapper.updateStatus(id,status);return getById(id);}
    private void validate(SeasonRequest r,Long id){if(r.endDate().isBefore(r.startDate()))throw new BusinessException("season end date must not be before start date");if(mapper.countByName(r.seasonName().trim(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"season name already exists");}
    private SeasonInfo copy(SeasonInfo s,SeasonRequest r){s.setSeasonName(r.seasonName().trim());s.setStartDate(r.startDate());s.setEndDate(r.endDate());s.setDescription(r.description()==null||r.description().isBlank()?null:r.description().trim());return s;}
}
