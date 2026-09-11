package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.PersonnelOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor @Transactional(readOnly=true)
public class ClubPersonnelServiceImpl implements ClubPersonnelService {
    private final PlayerInfoMapper playerMapper;
    private final CoachInfoMapper coachMapper;
    private final ClubHomeStadiumService homeService;
    private final SystemTimeService timeService;

    @Override public PersonnelOverviewResponse overview(Long clubId){
        List<PlayerInfo> players=playerMapper.findByClubId(clubId);List<CoachInfo> coaches=coachMapper.findByClubId(clubId);
        boolean homeComplete=homeService.profile(clubId).standardHomeComplete();
        int starters=(int)players.stream().filter(p->active(p)&&"STARTER".equals(p.getLineupRole())).count();
        int substitutes=(int)players.stream().filter(p->active(p)&&"SUBSTITUTE".equals(p.getLineupRole())).count();
        int keepers=(int)players.stream().filter(p->active(p)&&"STARTER".equals(p.getLineupRole())&&"GOALKEEPER".equals(p.getPosition())).count();
        int heads=(int)coaches.stream().filter(c->active(c)&&"HEAD_COACH".equals(c.getTitle())).count();
        int assistants=(int)coaches.stream().filter(c->active(c)&&"ASSISTANT_COACH".equals(c.getTitle())).count();
        String reason=!homeComplete?"主场/场地结构不完整":keepers!=1?"首发门将必须恰好1名":starters!=11?"首发球员必须恰好11名":substitutes>7?"替补球员最多7名":heads!=1?"现役主教练必须恰好1名":assistants>2?"现役副教练最多2名":null;
        int year=timeService.now().getYear();
        List<PersonnelOverviewResponse.Player> playerRows=players.stream().map(p->{Integer birth=p.getBirthYear()!=null?p.getBirthYear():(p.getBirthDate()==null?null:p.getBirthDate().getYear());return new PersonnelOverviewResponse.Player(p.getPlayerId(),p.getPlayerName(),birth,birth==null?null:year-birth,p.getNationality(),p.getPosition(),p.getLineupRole(),active(p)?"在队":"离队",p.getShirtNo());}).toList();
        List<PersonnelOverviewResponse.Coach> coachRows=coaches.stream().map(c->new PersonnelOverviewResponse.Coach(c.getCoachId(),c.getCoachName(),c.getBirthYear(),c.getBirthYear()==null?null:year-c.getBirthYear(),c.getNationality(),c.getTitle(),active(c)?"现役":"离队")).toList();
        return new PersonnelOverviewResponse(reason==null,reason==null?"队伍符合参赛标准":"当前队伍暂不符合比赛标准，无法报名任何比赛",reason,homeComplete,starters,substitutes,(int)coaches.stream().filter(this::active).count(),playerRows,coachRows);
    }
    @Override public PersonnelOverviewResponse requireCompliant(Long clubId){PersonnelOverviewResponse result=overview(clubId);if(!result.compliant())throw new BusinessException(result.message()+"："+result.reason());return result;}
    private boolean active(PlayerInfo p){return "ACTIVE".equals(p.getPlayerStatus());}
    private boolean active(CoachInfo c){return "ACTIVE".equals(c.getCoachStatus());}
}
