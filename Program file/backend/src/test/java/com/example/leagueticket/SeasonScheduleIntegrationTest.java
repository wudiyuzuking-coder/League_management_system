package com.example.leagueticket;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.ScheduleDetailResponse;
import com.example.leagueticket.vo.UserSeasonScheduleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class SeasonScheduleIntegrationTest {
    @Autowired JdbcTemplate jdbc; @Autowired SeasonScheduleService schedules; @Autowired SeasonInfoService seasons; @Autowired SeasonLifecycleService lifecycle;
    @Autowired MatchInfoService matches;
    @Autowired ClubSeasonEnrollmentService enrollments; @Autowired SystemTimeService time; @Autowired MockMvc mvc;
    @Autowired ObjectMapper json; @Autowired PasswordEncoder encoder;
    @Autowired com.example.leagueticket.task.ScheduleGenerationTask task; @Autowired ApplicationEventPublisher publisher; @Autowired TransactionTemplate transactions;

    @BeforeEach void setup(){cleanup();jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");}
    @AfterEach void tearDown(){jdbc.update("UPDATE sys_config SET config_value='0' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");cleanup();}

    @Test void fourClubDoubleRoundRobinConfirmAndVenueSnapshot(){
        long season=season("IT16C四队",4,LocalDate.of(2040,3,1),LocalDate.of(2040,5,1),LocalDateTime.now().minusDays(1));
        List<Team> teams=teams(season,4);ScheduleDetailResponse generated=schedules.generateIfEligible(season,"MANUAL");
        assertThat(generated.getBatchStatus()).isEqualTo("CONFIRMED");assertThat(generated.getRoundCount()).isEqualTo(6);assertThat(generated.getMatchCount()).isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?",String.class,season)).isEqualTo("PREPARING");
        assertThat(schedules.list(new ScheduleQueryRequest(season,"CONFIRMED",1,20)).total()).isEqualTo(1);
        MatchQueryRequest publicQuery=new MatchQueryRequest();publicQuery.setSeasonId(season);assertThat(matches.listPublic(publicQuery).total()).isEqualTo(12);
        assertRoundRobin(season,teams,6,12,3,3);assertVenuesUseEnrollmentSnapshot(season);
        UserSeasonScheduleResponse publicSchedule=schedules.getPublicConfirmed(season);
        assertThat(publicSchedule.rounds()).hasSize(6);
        assertThat(publicSchedule.rounds().stream().flatMap(round->round.matches().stream()).toList()).hasSize(12).allSatisfy(match->assertThat(match.getMatchStatus()).isEqualTo("PUBLISHED"));
        assertThat(schedules.clubSchedules(teams.get(0).clubId())).hasSize(6);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,season)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?",Integer.class,season)).isEqualTo(96);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory i JOIN match_info m ON m.match_id=i.match_id WHERE m.season_id=?",Integer.class,season)).isEqualTo(96);
        schedules.generateIfEligible(season,"FULL");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,season)).isEqualTo(4);
    }

    @Test void completeSeasonLifecycleRequiresConfirmedScheduleAndFinishedMatches(){
        long season=season("IT16C完整生命周期",2,LocalDate.of(2039,3,1),LocalDate.of(2039,5,1),time.now().plusDays(10));
        jdbc.update("UPDATE season_info SET season_status='DRAFT' WHERE season_id=?",season);
        assertThat(lifecycle.openRegistration(season).getSeasonStatus()).isEqualTo("REGISTRATION");
        teams(season,2);
        assertThat(schedules.closeRegistrationAndPublishSchedule(season,eventAdminId()).getBatchStatus()).isEqualTo("CONFIRMED");
        assertThat(seasons.getById(season).getSeasonStatus()).isEqualTo("PREPARING");
        long offset=Duration.between(time.realNow(),LocalDate.of(2039,3,1).atStartOfDay()).plusNanos(999_999_999).getSeconds();
        jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",Long.toString(offset));
        lifecycle.startInProgress(season);
        assertThatThrownBy(()->lifecycle.finish(season)).hasMessage("所有比赛完成后才能结束赛季");
        jdbc.update("UPDATE match_info SET match_status='FINISHED',home_score=1,away_score=0 WHERE season_id=?",season);
        assertThat(lifecycle.finish(season).getSeasonStatus()).isEqualTo("FINISHED");
    }

    @Test void publicSeasonListHidesUnconfirmedDraftAndReturnsConfirmedCounts(){
        long season=season("IT16C公开赛季",4,LocalDate.of(2039,3,1),LocalDate.of(2039,5,1),time.now().plusDays(2));
        teams(season,4);
        assertThat(seasons.listPublic()).noneMatch(row->row.getSeasonId().equals(season));

        schedules.generateIfEligible(season,"FULL");
        var row=seasons.listPublic().stream().filter(value->value.getSeasonId().equals(season)).findFirst().orElseThrow();
        assertThat(row.isScheduleConfirmed()).isTrue();
        assertThat(row.getTeamCount()).isEqualTo(4);
        assertThat(row.getRoundCount()).isEqualTo(6);
        assertThat(row.getMatchCount()).isEqualTo(12);
        assertThat(row.getPublicStatus()).isEqualTo("SCHEDULE_PUBLISHED");
    }

    @Test void publishedRoundIsVisibleWhileSeasonRemainsPreparing() throws Exception {
        long season=season("IT16C轮次公开校验",2,LocalDate.of(2039,7,1),LocalDate.of(2039,9,1),time.now().minusMinutes(1));
        teams(season,2);schedules.generateIfEligible(season,"DEADLINE");
        long roundId=jdbc.queryForObject("SELECT round_id FROM round_info WHERE season_id=? ORDER BY round_no LIMIT 1",Long.class,season);
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username='demo_user'",hash);
        String userToken=loginByPhone("13800000001");
        mvc.perform(get("/api/rounds/{id}",roundId).header("Authorization",bearer(userToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.roundId").value((int)roundId));
    }

    @Test void earlyCloseGeneratesBatchAndEnforcesPermissions() throws Exception {
        long season=season("IT16C提前截止",4,LocalDate.of(2039,10,1),LocalDate.of(2039,12,1),time.now().plusDays(10));teams(season,2);
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_club','demo_event_admin','demo_admin')",hash);
        for(String phone:List.of("13800000001","13800000003","13800000002"))
            mvc.perform(post("/api/admin/seasons/{id}/close-registration",season).header("Authorization",bearer(loginByPhone(phone)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/seasons/{id}/close-registration",season).header("Authorization",bearer(loginByPhone("13800000005"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.batchStatus").value("CONFIRMED")).andExpect(jsonPath("$.data.triggerType").value("MANUAL"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);
    }

    @Test void registrationClosePublishesTheCompleteScheduleAndTicketing() throws Exception {
        long season=season("IT16C统一关闭",4,LocalDate.of(2039,11,1),LocalDate.of(2040,2,1),time.now().plusDays(10));teams(season,2);
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username='demo_event_admin'",hash);
        mvc.perform(post("/api/admin/seasons/{id}/registration/close",season).header("Authorization",bearer(loginByPhone("13800000005"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.batchStatus").value("CONFIRMED"));
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?",String.class,season)).isEqualTo("PREPARING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM round_info WHERE season_id=? AND round_status<>'PUBLISHED'",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND match_status<>'PUBLISHED'",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,season)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=? AND z.zone_status='ON_SALE'",Integer.class,season)).isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory i JOIN match_info m ON m.match_id=i.match_id WHERE m.season_id=?",Integer.class,season)).isEqualTo(16);
    }

    @Test void earlyCloseRejectsInsufficientTeamsWithoutResidue(){
        long season=season("IT16C提前截止单队",4,LocalDate.of(2040,7,1),LocalDate.of(2040,9,1),time.now().plusDays(10));teams(season,1);
        assertThatThrownBy(()->schedules.closeRegistrationAndPublishSchedule(season,eventAdminId())).hasMessage("当前报名球队不足，无法生成赛程");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isZero();
    }

    @Test void incompleteStandardHomeRollsBackTheWholePublication(){
        long season=season("IT16C票区回滚",2,LocalDate.of(2040,8,1),LocalDate.of(2040,10,1),time.now().plusDays(10));List<Team> values=teams(season,2);
        long zone=jdbc.queryForObject("SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id=? ORDER BY stadium_zone_id LIMIT 1",Long.class,values.get(0).stadiumId());
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id=?",zone);jdbc.update("DELETE FROM stadium_zone WHERE stadium_zone_id=?",zone);
        assertThatThrownBy(()->schedules.closeRegistrationAndPublishSchedule(season,eventAdminId())).hasMessage("主场票区配置不完整，无法发布赛程");
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?",String.class,season)).isEqualTo("REGISTRATION");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM round_info WHERE season_id=?",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?",Integer.class,season)).isZero();
    }

    @Test void concurrentEarlyCloseCreatesOnlyOneBatch() throws Exception {
        long season=season("IT16C并发提前截止",4,LocalDate.of(2040,10,1),LocalDate.of(2040,12,1),time.now().plusDays(10));teams(season,2);
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{
            Callable<Long> call=()->{gate.await(5,TimeUnit.SECONDS);return schedules.closeRegistrationAndPublishSchedule(season,eventAdminId()).getBatchId();};
            Future<Long>a=pool.submit(call),b=pool.submit(call);
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(2);
    }

    @Test void fiveClubByeNeverPersistsAndEveryPairReverses(){
        long season=season("IT16C五队",5,LocalDate.of(2041,3,1),LocalDate.of(2041,6,1),LocalDateTime.now().minusDays(1));List<Team> teams=teams(season,5);
        ScheduleDetailResponse out=schedules.generateIfEligible(season,"MANUAL");assertThat(out.getRoundCount()).isEqualTo(10);assertThat(out.getMatchCount()).isEqualTo(20);
        assertRoundRobin(season,teams,10,20,4,4);
        assertThat(jdbc.queryForObject("SELECT MIN(c) FROM (SELECT round_id,COUNT(*) c FROM match_info WHERE season_id=? GROUP BY round_id) x",Integer.class,season)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT MAX(c) FROM (SELECT round_id,COUNT(*) c FROM match_info WHERE season_id=? GROUP BY round_id) x",Integer.class,season)).isEqualTo(2);
    }

    @Test void deadlineAllowsPartialFieldButEarlyAndSingleClubReject(){
        LocalDateTime future=time.now().plusDays(2);long early=season("IT16C未截止",4,LocalDate.of(2042,3,1),LocalDate.of(2042,5,1),future);teams(early,2);
        assertThatThrownBy(()->schedules.generateIfEligible(early,"MANUAL")).hasMessageContaining("未满额且报名尚未截止");
        long deadline=season("IT16C已截止",4,LocalDate.of(2043,3,1),LocalDate.of(2043,5,1),time.now().minusMinutes(1));teams(deadline,2);
        assertThat(schedules.generateIfEligible(deadline,"DEADLINE").getMatchCount()).isEqualTo(2);
        long single=season("IT16C单队",4,LocalDate.of(2044,3,1),LocalDate.of(2044,5,1),time.now().minusMinutes(1));teams(single,1);
        assertThatThrownBy(()->schedules.generateIfEligible(single,"DEADLINE")).hasMessageContaining("至少需要2支球队");
    }

    @Test void capacityValidationAndTimeRollbackCannotReopenEnrollment(){
        SeasonRequest shortSeason=new SeasonRequest("IT16C短赛季",LocalDate.of(2045,3,1),4);
        assertThat(seasons.create(shortSeason).getEndDate()).isAfter(shortSeason.startDate());
        long season=season("IT16C回拨",2,LocalDate.of(2046,3,1),LocalDate.of(2046,4,1),time.now().plusDays(1));List<Team> ts=teams(season,2);schedules.generateIfEligible(season,"FULL");
        jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",Long.toString(Duration.between(time.realNow(),time.realNow().minusDays(10)).getSeconds()));
        assertThat(enrollments.availableSeasons(ts.get(0).clubId()).stream().noneMatch(s->s.getSeasonId().equals(season))).isTrue();
        assertThatThrownBy(()->enrollments.submit(ts.get(0).clubId(),new EnrollmentRequest(season))).hasMessage("赛程已生成，不能继续报名");
        schedules.generateIfEligible(season,"MANUAL");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);
    }

    @Test void concurrentGenerateLeavesOneCompleteBatch() throws Exception {
        long season=season("IT16C并发",4,LocalDate.of(2047,3,1),LocalDate.of(2047,5,1),time.now().minusDays(1));teams(season,4);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{Callable<Long> call=()->{gate.await(5,TimeUnit.SECONDS);return schedules.generateIfEligible(season,"MANUAL").getBatchId();};Future<Long>a=pool.submit(call),b=pool.submit(call);assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(12);
    }

    @Test void automaticTicketingUsesTheExistingSaleWindowWithoutManualEnable() throws Exception {
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username='demo_user'",hash);String user=loginByPhone("13800000001");
        LocalDate today=time.now().toLocalDate();
        long within=season("IT16C立即销售",2,today.plusDays(7),today.plusMonths(2),time.now().minusMinutes(1));teams(within,2);schedules.generateIfEligible(within,"DEADLINE");
        long withinZone=jdbc.queryForObject("SELECT MIN(z.match_zone_id) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=? AND m.match_time=(SELECT MIN(match_time) FROM match_info WHERE season_id=?)",Long.class,within,within);
        mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",withinZone).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":1}"))
            .andExpect(status().isOk());

        long early=season("IT16C尚未开售",2,today.plusDays(30),today.plusMonths(3),time.now().minusMinutes(1));teams(early,2);schedules.generateIfEligible(early,"DEADLINE");
        long earlyZone=jdbc.queryForObject("SELECT MIN(z.match_zone_id) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=? AND m.match_time=(SELECT MIN(match_time) FROM match_info WHERE season_id=?)",Long.class,early,early);
        mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",earlyZone).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("ticket sales have not started"));

        long ended=season("IT16C已经停售",2,today.minusDays(1),today.plusMonths(1),time.now().minusDays(10));teams(ended,2);schedules.generateIfEligible(ended,"DEADLINE");
        long endedZone=jdbc.queryForObject("SELECT MIN(z.match_zone_id) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=? AND m.match_time=(SELECT MIN(match_time) FROM match_info WHERE season_id=?)",Long.class,ended,ended);
        mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",endedZone).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("ticket sales have ended"));
    }

    @Test void concurrentTrimmedSeasonNamesLeaveOneSeasonAndOneConflict() throws Exception {
        LocalDate start=time.now().plusYears(2).toLocalDate();ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{
            Callable<String> plain=()->createSeasonConcurrently(gate,"IT16C并发重名",start);
            Callable<String> padded=()->createSeasonConcurrently(gate,"  IT16C并发重名  ",start);
            Future<String> first=pool.submit(plain),second=pool.submit(padded);List<String> results=List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder("SUCCESS","赛季名称已存在，请添加编号后重试");
        }finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_info WHERE season_name='IT16C并发重名'",Integer.class)).isEqualTo(1);
    }

    @Test void onlyEventAdminCanGenerateAndConfirm() throws Exception {
        long season=season("IT16C权限",4,LocalDate.of(2048,3,1),LocalDate.of(2048,5,1),time.now().plusDays(2));teams(season,2);
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_club','demo_event_admin','demo_admin')",hash);
        for(String phone:List.of("13800000001","13800000003","13800000002"))mvc.perform(post("/api/admin/seasons/{id}/schedule/generate",season).header("Authorization",bearer(loginByPhone(phone)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/seasons/{id}/schedule/generate",season).header("Authorization",bearer(loginByPhone("13800000005")))).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/seasons/{id}/schedule/confirm",season).header("Authorization",bearer(loginByPhone("13800000005")))).andExpect(status().isNotFound());
    }

    @Test void afterCommitFullEventAndSchedulerDeadlineScanGenerateAutomatically(){
        long full=season("IT16C满额事件",4,LocalDate.of(2049,3,1),LocalDate.of(2049,5,1),time.now().plusDays(2));teams(full,4);
        transactions.executeWithoutResult(status->publisher.publishEvent(new ScheduleEligibilityEvent(full)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,full)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT batch_status FROM season_schedule_batch WHERE season_id=?",String.class,full)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT trigger_type FROM season_schedule_batch WHERE season_id=?",String.class,full)).isEqualTo("FULL");
        long expired=season("IT16C截止扫描",4,LocalDate.of(2050,3,1),LocalDate.of(2050,5,1),time.now().minusMinutes(1));teams(expired,2);
        task.deadlineScan();
        assertThat(jdbc.queryForObject("SELECT trigger_type FROM season_schedule_batch WHERE season_id=?",String.class,expired)).isEqualTo("DEADLINE");
        assertThat(jdbc.queryForObject("SELECT batch_status FROM season_schedule_batch WHERE season_id=?",String.class,expired)).isEqualTo("CONFIRMED");
    }

    private void assertRoundRobin(long season,List<Team> teams,int rounds,int matches,int home,int away){
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT round_id) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(rounds);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(matches);
        for(Team t:teams){assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND (home_club_id=? OR away_club_id=?)",Integer.class,season,t.clubId(),t.clubId())).isEqualTo(home+away);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND home_club_id=?",Integer.class,season,t.clubId())).isEqualTo(home);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND away_club_id=?",Integer.class,season,t.clubId())).isEqualTo(away);assertThat(jdbc.queryForObject("SELECT MAX(c) FROM (SELECT round_id,COUNT(*) c FROM match_info WHERE season_id=? AND (home_club_id=? OR away_club_id=?) GROUP BY round_id) x",Integer.class,season,t.clubId(),t.clubId())).isEqualTo(1);}
        assertThat(jdbc.queryForObject("SELECT MIN(gap_days) FROM (SELECT DATEDIFF(match_time,LAG(match_time) OVER(PARTITION BY club_id ORDER BY match_time)) gap_days FROM (SELECT home_club_id club_id,match_time FROM match_info WHERE season_id=? UNION ALL SELECT away_club_id,match_time FROM match_info WHERE season_id=?) z) q WHERE gap_days IS NOT NULL",Integer.class,season,season)).isGreaterThanOrEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT LEAST(home_club_id,away_club_id) a,GREATEST(home_club_id,away_club_id) b,COUNT(*) c,COUNT(DISTINCT home_club_id) h FROM match_info WHERE season_id=? GROUP BY a,b HAVING c<>2 OR h<>2) x",Integer.class,season)).isZero();
    }
    private void assertVenuesUseEnrollmentSnapshot(long season){assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info m JOIN club_season_enrollment e ON e.season_id=m.season_id AND e.club_id=m.home_club_id WHERE m.season_id=? AND m.stadium_id<>e.stadium_id",Integer.class,season)).isZero();}
    private long season(String name,int max,LocalDate start,LocalDate end,LocalDateTime deadline){jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,?,'REGISTRATION')",name,start,end,deadline.minusMonths(2),deadline,max);return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?",Long.class,name);}
    private List<Team> teams(long season,int n){List<Team> out=new ArrayList<>();for(int i=1;i<=n;i++){String suffix=season+"-"+i;jdbc.update("INSERT INTO stadium_info(stadium_name,city,address,capacity,venue_model,stadium_status) VALUES(?,?,?,8,'STANDARD_8','ACTIVE')","IT16C场馆"+suffix,"测试城","测试路"+i);long stadium=jdbc.queryForObject("SELECT stadium_id FROM stadium_info WHERE stadium_name=?",Long.class,"IT16C场馆"+suffix);jdbc.update("INSERT INTO club_info(club_name,home_city,home_stadium_id,club_status) VALUES(?,?,?,'ACTIVE')","IT16C俱乐部"+suffix,"测试城",stadium);long club=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name=?",Long.class,"IT16C俱乐部"+suffix);jdbc.update("INSERT INTO club_home_stadium_config(club_id,stadium_id,rows_per_zone,long_side_seats_per_row,short_side_seats_per_row,vip_price,normal_price) VALUES(?,?,1,1,1,200,100)",club,stadium);String[] directions={"EAST","WEST","SOUTH","NORTH"};String[] types={"VIP","NORMAL"};int sort=0;for(String direction:directions)for(String type:types){String code=direction+"_"+type;jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,zone_direction,ticket_type,sort_order,zone_status) VALUES(?,?,?,?,?,?,'ACTIVE')",stadium,code,code,direction,type,++sort);long zone=jdbc.queryForObject("SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id=? AND zone_code=?",Long.class,stadium,code);jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'A',1,'1',1,0,'ACTIVE')",stadium,zone);}jdbc.update("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(?,?,?,'SUBMITTED',?)",season,club,stadium,time.now());out.add(new Team(club,stadium));}return out;}
    private long eventAdminId(){return jdbc.queryForObject("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE r.role_code='EVENT_ADMIN' ORDER BY u.user_id LIMIT 1",Long.class);}
    private String createSeasonConcurrently(CyclicBarrier gate,String name,LocalDate start)throws Exception{gate.await(5,TimeUnit.SECONDS);try{seasons.create(new SeasonRequest(name,start,4));return "SUCCESS";}catch(com.example.leagueticket.exception.BusinessException e){return e.getMessage();}}
    private String loginByPhone(String phone)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private static String bearer(String token){return "Bearer "+token;}
    private void cleanup(){jdbc.update("DELETE FROM club_season_notification WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%'))");jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%'))");jdbc.update("DELETE sm FROM season_schedule_match sm JOIN match_info m ON m.match_id=sm.match_id JOIN season_info s ON s.season_id=m.season_id WHERE s.season_name LIKE 'IT16C%'");jdbc.update("DELETE FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'IT16C%'");jdbc.update("DELETE FROM stadium_seat WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'IT16C%')");jdbc.update("DELETE FROM stadium_zone WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_home_stadium_config WHERE club_id IN (SELECT club_id FROM club_info WHERE club_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_info WHERE club_name LIKE 'IT16C%'");jdbc.update("DELETE FROM stadium_info WHERE stadium_name LIKE 'IT16C%'");}
    private record Team(long clubId,long stadiumId){}
}
