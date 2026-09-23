package com.example.leagueticket.service;

import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.impl.SeasonScheduleServiceImpl;
import com.example.leagueticket.vo.ScheduleMatchResponse;
import com.example.leagueticket.vo.UserSeasonScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeasonScheduleVisibilityTest {
    @Test void confirmedScheduleReturnsEveryMatchRegardlessOfPurchasability(){
        SeasonInfoMapper seasons=mock(SeasonInfoMapper.class);
        SeasonScheduleMapper schedules=mock(SeasonScheduleMapper.class);
        SystemTimeService time=mock(SystemTimeService.class);
        LocalDateTime now=LocalDateTime.of(2026,11,5,20,1);
        TicketSalePolicy policy=new TicketSalePolicy(time);
        PublicSeasonVisibilityService visibility=mock(PublicSeasonVisibilityService.class);
        SeasonScheduleServiceImpl service=new SeasonScheduleServiceImpl(seasons,schedules,mock(RoundInfoMapper.class),mock(MatchInfoMapper.class),mock(ClubSeasonRecordMapper.class),time,mock(DoubleRoundRobinSchedulePlanner.class),policy,visibility,mock(SeasonLifecycleService.class));

        SeasonInfo season=new SeasonInfo();season.setSeasonId(1L);season.setSeasonName("已确认赛季");season.setStartDate(LocalDate.of(2026,11,1));season.setEndDate(LocalDate.of(2026,12,31));
        SeasonScheduleBatch batch=new SeasonScheduleBatch();batch.setSeasonId(1L);batch.setBatchStatus("CONFIRMED");batch.setClubCount(4);
        when(seasons.findById(1L)).thenReturn(season);when(schedules.findBySeason(1L)).thenReturn(batch);when(time.now()).thenReturn(now);
        when(schedules.findConfirmedPublicMatches(1L)).thenReturn(List.of(
                match(1L,1,"PUBLISHED",LocalDateTime.of(2026,11,25,20,0),10,1),
                match(2L,1,"PUBLISHED",LocalDateTime.of(2026,11,15,20,0),10,1),
                match(3L,2,"PUBLISHED",LocalDateTime.of(2026,11,5,20,0),10,1),
                match(4L,2,"FINISHED",LocalDateTime.of(2026,11,1,20,0),0,0)));

        UserSeasonScheduleResponse result=service.getPublicConfirmed(1L);
        List<ScheduleMatchResponse> matches=result.rounds().stream().flatMap(round->round.matches().stream()).toList();
        assertThat(matches).extracting(ScheduleMatchResponse::getSaleStatus).containsExactly("NOT_STARTED","ON_SALE","ENDED","MATCH_UNAVAILABLE");
        assertThat(matches).extracting(ScheduleMatchResponse::isPurchasable).containsExactly(false,true,false,false);
        assertThat(matches).extracting(ScheduleMatchResponse::getMatchId).containsExactly(1L,2L,3L,4L);
    }

    @Test void unconfirmedScheduleIsNotPublic(){
        SeasonInfoMapper seasons=mock(SeasonInfoMapper.class);SeasonScheduleMapper schedules=mock(SeasonScheduleMapper.class);SystemTimeService time=mock(SystemTimeService.class);
        SeasonInfo season=new SeasonInfo();season.setSeasonId(2L);SeasonScheduleBatch batch=new SeasonScheduleBatch();batch.setBatchStatus("GENERATED");
        when(seasons.findById(2L)).thenReturn(season);when(schedules.findBySeason(2L)).thenReturn(batch);
        PublicSeasonVisibilityService visibility=mock(PublicSeasonVisibilityService.class);
        doThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,"public season not found")).when(visibility).requirePublicVisibleSeason(2L);
        SeasonScheduleServiceImpl service=new SeasonScheduleServiceImpl(seasons,schedules,mock(RoundInfoMapper.class),mock(MatchInfoMapper.class),mock(ClubSeasonRecordMapper.class),time,mock(DoubleRoundRobinSchedulePlanner.class),new TicketSalePolicy(time),visibility,mock(SeasonLifecycleService.class));
        assertThatThrownBy(()->service.getPublicConfirmed(2L)).isInstanceOf(BusinessException.class).hasMessage("public season not found");
        verify(schedules,never()).findConfirmedPublicMatches(2L);
    }

    private ScheduleMatchResponse match(long id,int round,String status,LocalDateTime time,long remaining,int onSaleZones){
        ScheduleMatchResponse row=new ScheduleMatchResponse();row.setMatchId(id);row.setRoundNo(round);row.setMatchStatus(status);row.setMatchDateTime(time);row.setRemainingTickets(remaining);row.setOnSaleZoneCount(onSaleZones);return row;
    }
}
