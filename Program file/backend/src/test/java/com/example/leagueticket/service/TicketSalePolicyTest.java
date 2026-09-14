package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TicketSalePolicyTest {
    private final TicketSalePolicy policy=new TicketSalePolicy(mock(SystemTimeService.class));

    @Test void queryPreviewAndOrderShareInclusiveFourteenDayStartAndExclusiveOneHourEnd(){
        LocalDateTime matchTime=LocalDateTime.of(2026,11,20,20,0),start=LocalDateTime.of(2026,11,6,20,0);
        MatchInfo match=new MatchInfo();match.setMatchStatus("PUBLISHED");match.setMatchTime(matchTime);policy.applySaleWindow(match);
        MatchTicketZone zone=new MatchTicketZone();zone.setZoneStatus("ON_SALE");
        assertThat(policy.calculateSaleStartTime(match)).isEqualTo(start);
        policy.validateSaleWindow(match);
        assertThat(policy.evaluateSaleAvailability(match,zone,LocalDateTime.of(2026,11,6,19,59,59),1).state()).isEqualTo("NOT_STARTED");
        assertThat(policy.evaluateSaleAvailability(match,zone,start,1).available()).isTrue();
        assertThat(policy.evaluateSaleAvailability(match,zone,LocalDateTime.of(2026,11,20,18,59,59),1).available()).isTrue();
        assertThat(policy.evaluateSaleAvailability(match,zone,matchTime.minusHours(1),1).state()).isEqualTo("ENDED");
        assertThat(policy.saleStopMinutes()).isEqualTo(60);
    }

    @Test void everyMatchUsesItsOwnDateAtTwentyRegardlessOfKickoffHour(){
        assertThat(policy.calculateSaleStartTime(LocalDateTime.of(2026,11,20,15,30))).isEqualTo(LocalDateTime.of(2026,11,6,20,0));
        assertThat(policy.calculateSaleStartTime(LocalDateTime.of(2026,11,27,21,15))).isEqualTo(LocalDateTime.of(2026,11,13,20,0));
    }
}
