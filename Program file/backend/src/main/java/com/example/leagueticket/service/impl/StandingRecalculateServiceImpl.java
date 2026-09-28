package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.ClubSeasonRecord;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubSeasonRecordMapper;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.StandingRecalculateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Service @Profile("dev") @RequiredArgsConstructor
public class StandingRecalculateServiceImpl implements StandingRecalculateService {
    private final ClubSeasonRecordMapper recordMapper;
    private final MatchInfoMapper matchMapper;
    private final SeasonInfoMapper seasonMapper;
    @Override @Transactional
    public void recalculate(Long seasonId){
        var season=seasonMapper.findById(seasonId);
        if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        if(SeasonStatus.CANCELLED.matches(season.getSeasonStatus()))throw new BusinessException(HttpStatus.CONFLICT,"已取消赛季不能重算积分榜");
        recordMapper.deleteNonParticipants(seasonId);
        recordMapper.initializeSeasonParticipants(seasonId);
        var participants=new HashSet<>(recordMapper.findParticipantClubIds(seasonId));
        var matches=matchMapper.findFinishedBySeason(seasonId);
        recordMapper.resetSeason(seasonId);
        Map<Long,Totals> totals=new HashMap<>();
        for(MatchInfo m:matches){if(!participants.contains(m.getHomeClubId())||!participants.contains(m.getAwayClubId()))continue;Totals home=totals.computeIfAbsent(m.getHomeClubId(),key->new Totals());Totals away=totals.computeIfAbsent(m.getAwayClubId(),key->new Totals());home.played++;away.played++;home.gf+=m.getHomeScore();home.ga+=m.getAwayScore();away.gf+=m.getAwayScore();away.ga+=m.getHomeScore();if(m.getHomeScore()>m.getAwayScore()){home.wins++;away.losses++;}else if(m.getHomeScore()<m.getAwayScore()){away.wins++;home.losses++;}else{home.draws++;away.draws++;}}
        totals.forEach((clubId,t)->{ClubSeasonRecord r=recordMapper.findBySeasonAndClub(seasonId,clubId);r.setPlayed(t.played);r.setWins(t.wins);r.setDraws(t.draws);r.setLosses(t.losses);r.setGoalsFor(t.gf);r.setGoalsAgainst(t.ga);r.setPoints(t.wins*3+t.draws);recordMapper.update(r);});
    }
    private static class Totals {int played,wins,draws,losses,gf,ga;}
}
