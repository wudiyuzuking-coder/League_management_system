package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.SeasonRecordRequest;
import com.example.leagueticket.entity.ClubSeasonRecord;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubSeasonRecordMapper;
import com.example.leagueticket.service.ClubSeasonRecordService;
import com.example.leagueticket.service.SeasonInfoService;
import com.example.leagueticket.vo.StandingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service @Profile("dev") @RequiredArgsConstructor
public class ClubSeasonRecordServiceImpl implements ClubSeasonRecordService {
    private final ClubSeasonRecordMapper mapper;
    private final SeasonInfoService seasonService;
    public List<StandingResponse> standings(Long seasonId){seasonService.getById(seasonId);return mapRows(mapper.findStandings(seasonId));}
    @Transactional public int initialize(Long seasonId){seasonService.getById(seasonId);return mapper.initializeActiveClubs(seasonId);}
    @Transactional public StandingResponse update(Long recordId,SeasonRecordRequest request){ClubSeasonRecord record=mapper.findById(recordId);if(record==null)throw new BusinessException(HttpStatus.NOT_FOUND,"club season record not found");record.setWins(request.wins());record.setDraws(request.draws());record.setLosses(request.losses());record.setGoalsFor(request.goalsFor());record.setGoalsAgainst(request.goalsAgainst());record.setPlayed(request.wins()+request.draws()+request.losses());record.setPoints(request.wins()*3+request.draws());mapper.update(record);return standings(record.getSeasonId()).stream().filter(v->v.recordId().equals(recordId)).findFirst().orElseThrow();}
    private List<StandingResponse> mapRows(List<ClubSeasonRecordMapper.RecordRow> rows){List<StandingResponse> result=new ArrayList<>();int rank=1;for(var row:rows){result.add(new StandingResponse(row.getRecordId(),row.getClubId(),row.getClubName(),row.getLogoUrl(),row.getPlayed(),row.getWins(),row.getDraws(),row.getLosses(),row.getGoalsFor(),row.getGoalsAgainst(),row.getGoalsFor()-row.getGoalsAgainst(),row.getPoints(),rank++));}return result;}
}
