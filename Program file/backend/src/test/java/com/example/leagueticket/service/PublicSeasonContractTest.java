package com.example.leagueticket.service;

import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.vo.PublicSeasonResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PublicSeasonContractTest {
    @Test void publicResponseExposesBusinessFieldsWithoutInternalSeasonStatus(){
        Set<String> fields=Arrays.stream(PublicSeasonResponse.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).collect(Collectors.toSet());
        assertThat(fields).contains("teamCount","roundCount","matchCount","scheduleConfirmed","publicStatus");
        assertThat(fields).doesNotContain("seasonStatus");
    }

    @Test void publicQueryUsesConfirmedScheduleOrStartedMatchInsteadOfSeasonStatus(){
        String sql=SeasonInfoMapper.PUBLIC_SEASON_SELECT;
        assertThat(sql).contains("b.batch_status='CONFIRMED'","matches.in_progress_count","matches.finished_count");
        assertThat(sql).doesNotContain("season_status");
    }
}
