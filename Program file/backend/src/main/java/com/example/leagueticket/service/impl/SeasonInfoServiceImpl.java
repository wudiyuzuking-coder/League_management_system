package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.SeasonRequest;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.RoundInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class SeasonInfoServiceImpl implements SeasonInfoService {
    private static final Map<String,String> NEXT=Map.of("DRAFT","ACTIVE","ACTIVE","FINISHED");
    private final SeasonInfoMapper mapper;
    private final RoundInfoMapper roundMapper;
    private final SystemTimeService timeService;
    private final DoubleRoundRobinSchedulePlanner planner;
    public List<SeasonInfo> list(){return mapper.findAll();}
    public SeasonInfo getById(Long id){SeasonInfo value=mapper.findById(id);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");return value;}
    @Transactional public SeasonInfo create(SeasonRequest request){validate(request);SeasonInfo s=derive(new SeasonInfo(),request);s.setSeasonStatus("DRAFT");mapper.insert(s);return getById(s.getSeasonId());}
    @Transactional public SeasonInfo update(Long id,SeasonRequest request){SeasonInfo s=getById(id);if(!"DRAFT".equals(s.getSeasonStatus()))throw new BusinessException(HttpStatus.CONFLICT,"only a DRAFT season can be edited");validate(request);if(!roundMapper.findBySeasonId(id).isEmpty())throw new BusinessException(HttpStatus.CONFLICT,"a scheduled season cannot be edited");mapper.update(derive(s,request));return getById(id);}
    @Transactional public SeasonInfo updateStatus(Long id,String status){SeasonInfo s=getById(id);if(s.getSeasonStatus().equals(status))return s;String next=NEXT.get(s.getSeasonStatus());if(!status.equals(next))throw new BusinessException("invalid season status transition: "+s.getSeasonStatus()+" -> "+status);mapper.updateStatus(id,status);return getById(id);}
    private void validate(SeasonRequest r){if(r.seasonName()==null||r.seasonName().trim().isEmpty())throw new BusinessException("赛季名称不能为空");if(r.maxClubs()<2||r.maxClubs()>20)throw new BusinessException("maxClubs must be between 2 and 20");LocalDate minimum=timeService.now().plusMonths(1).toLocalDate();if(r.startDate().isBefore(minimum))throw new BusinessException("赛季开始日期必须不早于系统时间一个月后");}
    private SeasonInfo derive(SeasonInfo s,SeasonRequest r){LocalDateTime now=timeService.now();LocalDateTime registrationStart=nextTwenty(now);LocalDateTime deadline=r.startDate().minusDays(15).atTime(19,59);if(!deadline.isAfter(registrationStart))throw new BusinessException("自动报名窗口不足，请选择更晚的赛季开始日期");s.setSeasonName(r.seasonName().trim());s.setStartDate(r.startDate());s.setEndDate(planner.expectedEndDate(r.maxClubs(),r.startDate()));s.setRegistrationStartTime(registrationStart);s.setRegistrationDeadline(deadline);s.setTicketSaleStartTime(deadline.toLocalDate().plusDays(1).atTime(20,0));s.setMaxClubs(r.maxClubs());s.setDescription(null);return s;}
    private LocalDateTime nextTwenty(LocalDateTime now){LocalDateTime today=now.toLocalDate().atTime(20,0);return now.isBefore(today)?today:today.plusDays(1);}
}
