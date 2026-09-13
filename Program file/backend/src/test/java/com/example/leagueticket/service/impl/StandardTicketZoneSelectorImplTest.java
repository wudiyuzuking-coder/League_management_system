package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.MatchTicketZoneMapper;
import com.example.leagueticket.service.MatchSeatInventoryService;
import com.example.leagueticket.service.TicketSalePolicy;
import com.example.leagueticket.vo.TicketZoneAvailabilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandardTicketZoneSelectorImplTest {
    @Mock MatchTicketZoneMapper zoneMapper;
    @Mock MatchInfoMapper matchMapper;
    @Mock MatchSeatInventoryService inventoryService;
    @Mock TicketSalePolicy policy;
    StandardTicketZoneSelectorImpl selector;
    MatchInfo match;

    @BeforeEach void setup(){selector=new StandardTicketZoneSelectorImpl(zoneMapper,matchMapper,inventoryService,policy);match=new MatchInfo();match.setMatchId(9L);when(matchMapper.findById(9L)).thenReturn(match);}

    @Test void selectsFirstDirectionThatCanCarryWholeOrder(){
        MatchTicketZone east=zone(1L,"EAST"),west=zone(2L,"WEST"),south=zone(3L,"SOUTH");
        when(zoneMapper.findByMatchForUpdate(9L)).thenReturn(List.of(south,west,east));
        available(east,2,2);available(west,10,4);available(south,10,4);
        assertThat(selector.select(9L,"vip",3,true).getMatchZoneId()).isEqualTo(2L);
        verify(zoneMapper).findByMatchForUpdate(9L);
    }

    @Test void refusesCrossDirectionAggregation(){
        MatchTicketZone east=zone(1L,"EAST"),west=zone(2L,"WEST");
        when(zoneMapper.findByMatch(9L)).thenReturn(List.of(east,west));
        available(east,2,2);available(west,2,2);
        assertThatThrownBy(()->selector.select(9L,"VIP",3,false)).isInstanceOf(BusinessException.class).hasMessage("没有任何单一实际票区能够承载本次购票数量");
    }

    @Test void selectsEastWhenItHasStockEvenIfFullContiguityIsUnavailable(){
        MatchTicketZone east=zone(1L,"EAST"),west=zone(2L,"WEST");
        when(zoneMapper.findByMatch(9L)).thenReturn(List.of(west,east));
        available(east,4,1);available(west,8,4);
        assertThat(selector.select(9L,"VIP",4,false).getMatchZoneId()).isEqualTo(1L);
    }

    private MatchTicketZone zone(long id,String direction){MatchTicketZone z=new MatchTicketZone();z.setMatchZoneId(id);z.setMatchId(9L);z.setTicketType("VIP");z.setZoneDirection(direction);return z;}
    private void available(MatchTicketZone z,long count,int continuous){when(inventoryService.availability(z.getMatchZoneId())).thenReturn(new TicketZoneAvailabilityResponse(count,count,0,0,0,continuous));when(policy.evaluateSaleAvailability(match,z,count)).thenReturn(new TicketSalePolicy.SaleEvaluation(true,"AVAILABLE"));}
}
