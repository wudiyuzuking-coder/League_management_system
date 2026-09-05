package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.RoundRequest;
import com.example.leagueticket.entity.RoundInfo;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.RoundInfoMapper;
import com.example.leagueticket.service.RoundInfoService;
import com.example.leagueticket.service.SeasonInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service @Profile("dev") @RequiredArgsConstructor
public class RoundInfoServiceImpl implements RoundInfoService {
    private static final Map<String,String> NEXT=Map.of("DRAFT","PUBLISHED","PUBLISHED","FINISHED");
    private final RoundInfoMapper mapper;
    private final SeasonInfoService seasonService;
    public List<RoundInfo> listBySeason(Long seasonId){seasonService.getById(seasonId);return mapper.findBySeasonId(seasonId);}
    public RoundInfo getById(Long id){RoundInfo value=mapper.findById(id);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"round not found");return value;}
    @Transactional public RoundInfo create(Long seasonId,RoundRequest request){SeasonInfo season=seasonService.getById(seasonId);validate(season,request,null);RoundInfo round=copy(new RoundInfo(),request);round.setSeasonId(seasonId);round.setRoundStatus("DRAFT");mapper.insert(round);return getById(round.getRoundId());}
    @Transactional public RoundInfo update(Long id,RoundRequest request){RoundInfo round=getById(id);SeasonInfo season=seasonService.getById(round.getSeasonId());validate(season,request,id);mapper.update(copy(round,request));return getById(id);}
    @Transactional public RoundInfo updateStatus(Long id,String status){RoundInfo round=getById(id);if(round.getRoundStatus().equals(status))return round;String next=NEXT.get(round.getRoundStatus());if(!status.equals(next))throw new BusinessException("invalid round status transition: "+round.getRoundStatus()+" -> "+status);mapper.updateStatus(id,status);return getById(id);}
    private void validate(SeasonInfo season,RoundRequest r,Long id){if(r.endDate().isBefore(r.startDate()))throw new BusinessException("round end date must not be before start date");if(r.startDate().isBefore(season.getStartDate())||r.endDate().isAfter(season.getEndDate()))throw new BusinessException("round dates must be within season date range");if(mapper.countByRoundNo(season.getSeasonId(),r.roundNo(),id)>0)throw new BusinessException(HttpStatus.CONFLICT,"round number already exists in this season");}
    private RoundInfo copy(RoundInfo round,RoundRequest r){round.setRoundNo(r.roundNo());round.setRoundName(r.roundName().trim());round.setStartDate(r.startDate());round.setEndDate(r.endDate());return round;}
}
