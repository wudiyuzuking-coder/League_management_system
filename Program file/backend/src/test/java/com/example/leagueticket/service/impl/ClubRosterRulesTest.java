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
import com.example.leagueticket.service.PersonnelAgeValidationService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.ClubProfileResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class ClubRosterRulesTest {
    @Test void playerAgeBoundariesUseSystemYear() {
        SystemTimeService time=mock(SystemTimeService.class);
        when(time.now()).thenReturn(LocalDateTime.of(2026,1,1,0,0));
        PersonnelAgeValidationService service=new PersonnelAgeValidationServiceImpl(time);

        assertThat(service.validatePlayerAge(null,2008)).isEqualTo(18);
        assertThatThrownBy(()->service.validatePlayerAge(null,2009)).hasMessage("球员年龄必须在18至50岁之间");
        assertThat(service.validatePlayerAge(null,1976)).isEqualTo(50);
        assertThatThrownBy(()->service.validatePlayerAge(null,1975)).hasMessage("球员年龄必须在18至50岁之间");
    }

    @Test void coachAgeBoundariesUseSystemYear() {
        SystemTimeService time=mock(SystemTimeService.class);
        when(time.now()).thenReturn(LocalDateTime.of(2026,1,1,0,0));
        PersonnelAgeValidationService service=new PersonnelAgeValidationServiceImpl(time);

        assertThat(service.validateCoachAge(1926)).isEqualTo(100);
        assertThatThrownBy(()->service.validateCoachAge(1925)).hasMessage("教练年龄必须在18至100岁之间");
    }

    @Test void eleventhStarterMustBeGoalkeeperWhenFirstTenHaveNone() {
        PlayerInfoMapper mapper=mock(PlayerInfoMapper.class);
        PlayerInfoServiceImpl service=playerService(mapper);
        when(mapper.countActiveRole(1L,"STARTER",null)).thenReturn(10);
        when(mapper.countStartingGoalkeepers(1L,null)).thenReturn(0);

        assertThatThrownBy(()->service.create(1L,player("普通球员",11,"FORWARD","STARTER")))
                .hasMessage("最后一个首发名额必须设置为门将");
    }

    @Test void eleventhStarterCanBeGoalkeeperWhenFirstTenHaveNone() {
        PlayerInfoMapper mapper=mock(PlayerInfoMapper.class);
        PlayerInfoServiceImpl service=playerService(mapper);
        stubInsertedPlayer(mapper);
        when(mapper.countActiveRole(1L,"STARTER",null)).thenReturn(10);
        when(mapper.countStartingGoalkeepers(1L,null)).thenReturn(0);

        assertThatCode(()->service.create(1L,player("新门将",11,"GOALKEEPER","STARTER"))).doesNotThrowAnyException();
    }

    @Test void eleventhStarterCanBeOutfieldPlayerWhenGoalkeeperAlreadyStarts() {
        PlayerInfoMapper mapper=mock(PlayerInfoMapper.class);
        PlayerInfoServiceImpl service=playerService(mapper);
        stubInsertedPlayer(mapper);
        when(mapper.countActiveRole(1L,"STARTER",null)).thenReturn(10);
        when(mapper.countStartingGoalkeepers(1L,null)).thenReturn(1);

        assertThatCode(()->service.create(1L,player("普通球员",11,"FORWARD","STARTER"))).doesNotThrowAnyException();
    }

    @Test void seventhSubstituteSucceedsAndEighthIsRejected() {
        PlayerInfoMapper mapper=mock(PlayerInfoMapper.class);
        PlayerInfoServiceImpl service=playerService(mapper);
        stubInsertedPlayer(mapper);
        when(mapper.countActiveRole(1L,"SUBSTITUTE",null)).thenReturn(6);
        assertThatCode(()->service.create(1L,player("第七替补",7,"FORWARD","SUBSTITUTE"))).doesNotThrowAnyException();

        when(mapper.countActiveRole(1L,"SUBSTITUTE",null)).thenReturn(7);
        assertThatThrownBy(()->service.create(1L,player("第八替补",8,"FORWARD","SUBSTITUTE"))).hasMessage("替补球员最多7名");
    }

    @Test void coachLimitsRequireOneHeadAndAtMostTwoAssistants() {
        CoachInfoMapper mapper=mock(CoachInfoMapper.class);
        CoachInfoServiceImpl service=new CoachInfoServiceImpl(mapper,mock(ClubInfoService.class),mock(ClubDataScopeService.class),mock(PersonnelAgeValidationService.class));
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

    private PlayerInfoServiceImpl playerService(PlayerInfoMapper mapper) {
        return new PlayerInfoServiceImpl(mapper,mock(ClubInfoService.class),mock(ClubDataScopeService.class),mock(PersonnelAgeValidationService.class));
    }

    private PlayerRequest player(String name,int shirtNo,String position,String lineupRole) {
        return new PlayerRequest(name,shirtNo,position,"中国",null,2000,lineupRole);
    }

    private void stubInsertedPlayer(PlayerInfoMapper mapper) {
        doAnswer(invocation->{
            PlayerInfo player=invocation.getArgument(0);
            player.setPlayerId(1L);
            return 1;
        }).when(mapper).insert(any(PlayerInfo.class));
        when(mapper.findById(1L)).thenAnswer(invocation->{
            PlayerInfo player=new PlayerInfo();
            player.setPlayerId(1L);
            return player;
        });
    }
}
