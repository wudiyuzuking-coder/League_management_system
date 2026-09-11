package com.example.leagueticket.service;
import org.junit.jupiter.api.Test;import java.time.LocalDate;import java.time.temporal.ChronoUnit;import java.util.*;import static org.assertj.core.api.Assertions.assertThat;
class DoubleRoundRobinSchedulePlannerTest {
 private final DoubleRoundRobinSchedulePlanner planner=new DoubleRoundRobinSchedulePlanner();
 @Test void evenAndOddSchedulesUseOneMatchPerDayTwentyTeamsIncluded(){for(int clubs:new int[]{2,3,4,5,20}){var games=planner.plan(clubs,LocalDate.of(2030,1,1));assertThat(games).hasSize(clubs*(clubs-1));assertThat(games.stream().map(DoubleRoundRobinSchedulePlanner.PlannedGame::date).distinct()).hasSize(games.size());Map<Integer,LocalDate> last=new HashMap<>();for(var g:games){for(int club:new int[]{g.homeIndex(),g.awayIndex()}){if(last.containsKey(club))assertThat(ChronoUnit.DAYS.between(last.get(club),g.date())).isGreaterThanOrEqualTo(6);last.put(club,g.date());}}assertThat(planner.expectedEndDate(clubs,LocalDate.of(2030,1,1))).isEqualTo(games.get(games.size()-1).date());}}
 @Test void everyPairPlaysHomeAndAway(){var games=planner.plan(5,LocalDate.of(2030,1,1));for(int a=0;a<5;a++)for(int b=0;b<5;b++)if(a!=b){int home=a,away=b;assertThat(games.stream().filter(g->g.homeIndex()==home&&g.awayIndex()==away)).hasSize(1);}}
}
