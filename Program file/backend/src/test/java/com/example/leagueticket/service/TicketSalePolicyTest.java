package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TicketSalePolicyTest {
    private final TicketSalePolicy policy=new TicketSalePolicy(mock(SystemTimeService.class));

    @Test void queryPreviewAndOrderShareInclusiveStartAndExclusiveOneHourEnd(){
        LocalDateTime start=LocalDateTime.of(2030,1,2,20,0),matchTime=LocalDateTime.of(2030,2,1,20,0);
        MatchInfo match=new MatchInfo();match.setMatchStatus("PUBLISHED");match.setMatchTime(matchTime);match.setSaleStartTime(start);match.setSaleEndTime(matchTime.minusHours(1));
        MatchTicketZone zone=new MatchTicketZone();zone.setZoneStatus("ON_SALE");zone.setSaleStartTime(start);zone.setSaleEndTime(matchTime.minusHours(1));
        policy.validateSaleWindow(match);
        assertThat(policy.evaluateSaleAvailability(match,zone,start.minusNanos(1),1).state()).isEqualTo("NOT_STARTED");
        assertThat(policy.evaluateSaleAvailability(match,zone,start,1).available()).isTrue();
        assertThat(policy.evaluateSaleAvailability(match,zone,matchTime.minusHours(1).minusNanos(1),1).available()).isTrue();
        assertThat(policy.evaluateSaleAvailability(match,zone,matchTime.minusHours(1),1).state()).isEqualTo("ENDED");
        assertThat(policy.saleStopMinutes()).isEqualTo(60);
    }
}
