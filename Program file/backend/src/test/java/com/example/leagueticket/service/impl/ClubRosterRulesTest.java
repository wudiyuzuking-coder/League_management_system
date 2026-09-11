package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.entity.CoachInfo;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.mapper.CoachInfoMapper;
import com.example.leagueticket.mapper.PlayerInfoMapper;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubHomeStadiumService;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.ClubProfileResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClubRosterRulesTest {
    @Test void playerLimitsProtectStartingGoalkeeperAndSubstituteCapacity() {
        PlayerInfoMapper mapper=mock(PlayerInfoMapper.class);
        ClubInfoService clubs=mock(ClubInfoService.class);
        PlayerInfoServiceImpl service=new PlayerInfoServiceImpl(mapper,clubs,mock(ClubDataScopeService.class));
        PlayerRequest goalkeeper=new PlayerRequest("新门将",1,"GOALKEEPER","中国",null,2001,"STARTER");
        when(mapper.countStartingGoalkeepers(1L,null)).thenReturn(1);
        assertThatThrownBy(()->service.create(1L,goalkeeper)).hasMessage("首发门将只能有1名");

        PlayerRequest substitute=new PlayerRequest("新替补",2,"FORWARD","中国",null,2002,"SUBSTITUTE");
        when(mapper.countActiveRole(1L,"SUBSTITUTE",null)).thenReturn(7);
        assertThatThrownBy(()->service.create(1L,substitute)).hasMessage("替补球员最多7名");
    }

    @Test void coachLimitsRequireOneHeadAndAtMostTwoAssistants() {
        CoachInfoMapper mapper=mock(CoachInfoMapper.class);
        CoachInfoServiceImpl service=new CoachInfoServiceImpl(mapper,mock(ClubInfoService.class),mock(ClubDataScopeService.class));
        when(mapper.countActiveTitle(1L,"HEAD_COACH",null)).thenReturn(1);
        assertThatThrownBy(()->service.create(1L,new CoachRequest("新主教练","HEAD_COACH","中国",1975,null))).hasMessage("现役主教练只能有1名");
        when(mapper.countActiveTitle(1L,"ASSISTANT_COACH",null)).thenReturn(2);
        assertThatThrownBy(()->service.create(1L,new CoachRequest("新副教练","ASSISTANT_COACH","中国",1980,null))).hasMessage("现役副教练最多2名");
    }

    @Test void complianceReasonUsesRequestedPriorityAndHistoricalBirthDateYear() {
        PlayerInfoMapper players=mock(PlayerInfoMapper.class);CoachInfoMapper coaches=mock(CoachInfoMapper.class);
        ClubHomeStadiumService home=mock(ClubHomeStadiumService.class);SystemTimeService time=mock(SystemTimeService.class);
        ClubProfileResponse profile=mock(ClubProfileResponse.class);when(home.profile(1L)).thenReturn(profile);when(profile.standardHomeComplete()).thenReturn(true);
        when(time.now()).thenReturn(LocalDateTime.of(2026,1,1,0,0));
        List<PlayerInfo> roster=new ArrayList<>();
        for(int i=0;i<10;i++){PlayerInfo p=new PlayerInfo();p.setPlayerId((long)i+1);p.setPlayerName("P"+i);p.setPlayerStatus("ACTIVE");p.setLineupRole("STARTER");p.setPosition("FORWARD");p.setBirthDate(java.time.LocalDate.of(2000,2,3));roster.add(p);}
        when(players.findByClubId(1L)).thenReturn(roster);when(coaches.findByClubId(1L)).thenReturn(List.of());
        var result=new ClubPersonnelServiceImpl(players,coaches,home,time).overview(1L);
        assertThat(result.compliant()).isFalse();
        assertThat(result.reason()).isEqualTo("首发门将必须恰好1名");
        assertThat(result.players().get(0).birthYear()).isEqualTo(2000);
        assertThat(result.players().get(0).age()).isEqualTo(26);
    }
}
