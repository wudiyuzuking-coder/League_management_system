package com.example.leagueticket;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.ScheduleDetailResponse;
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
    @Autowired JdbcTemplate jdbc; @Autowired SeasonScheduleService schedules; @Autowired SeasonInfoService seasons;
    @Autowired MatchInfoService matches;
    @Autowired ClubSeasonEnrollmentService enrollments; @Autowired SystemTimeService time; @Autowired MockMvc mvc;
    @Autowired ObjectMapper json; @Autowired PasswordEncoder encoder;
    @Autowired com.example.leagueticket.task.ScheduleGenerationTask task; @Autowired ApplicationEventPublisher publisher; @Autowired TransactionTemplate transactions;

    @BeforeEach void setup(){cleanup();jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");}
    @AfterEach void tearDown(){jdbc.update("UPDATE sys_config SET config_value='0' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");cleanup();}

    @Test void fourClubDoubleRoundRobinConfirmAndVenueSnapshot(){
        long season=season("IT16C四队",4,LocalDate.of(2040,3,1),LocalDate.of(2040,5,1),LocalDateTime.now().minusDays(1));
        List<Team> teams=teams(season,4);ScheduleDetailResponse generated=schedules.generateIfEligible(season,"MANUAL");
        assertThat(generated.getBatchStatus()).isEqualTo("GENERATED");assertThat(generated.getRoundCount()).isEqualTo(6);assertThat(generated.getMatchCount()).isEqualTo(12);
        assertThat(schedules.list(new ScheduleQueryRequest(season,"GENERATED",1,20)).total()).isEqualTo(1);
        MatchQueryRequest publicQuery=new MatchQueryRequest();publicQuery.setSeasonId(season);assertThat(matches.listPublic(publicQuery).total()).isZero();
        long draftMatch=jdbc.queryForObject("SELECT match_id FROM match_info WHERE season_id=? ORDER BY match_id LIMIT 1",Long.class,season);assertThatThrownBy(()->matches.getPublicById(draftMatch)).hasMessage("match not found");
        assertRoundRobin(season,teams,6,12,3,3);assertVenuesUseEnrollmentSnapshot(season);
        assertThat(schedules.clubSchedules(teams.get(0).clubId())).isEmpty();
        ScheduleDetailResponse confirmed=schedules.confirm(season,eventAdminId());assertThat(confirmed.getBatchStatus()).isEqualTo("CONFIRMED");
        assertThat(schedules.clubSchedules(teams.get(0).clubId())).hasSize(6);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,season)).isEqualTo(4);
        schedules.confirm(season,eventAdminId());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,season)).isEqualTo(4);
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
        SeasonRequest shortSeason=new SeasonRequest("IT16C日期不足",LocalDate.of(2045,3,1),LocalDate.of(2045,3,20),LocalDateTime.of(2045,1,1,0,0),LocalDateTime.of(2045,2,20,0,0),4,null);
        assertThatThrownBy(()->seasons.create(shortSeason)).hasMessageContaining("insufficient");
        long season=season("IT16C回拨",2,LocalDate.of(2046,3,1),LocalDate.of(2046,4,1),time.now().minusDays(1));List<Team> ts=teams(season,2);schedules.generateIfEligible(season,"FULL");
        jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",Long.toString(Duration.between(time.realNow(),time.realNow().minusDays(10)).getSeconds()));
        assertThat(enrollments.availableSeasons(ts.get(0).clubId()).stream().noneMatch(s->s.getSeasonId().equals(season))).isTrue();
        assertThatThrownBy(()->enrollments.submit(ts.get(0).clubId(),new EnrollmentRequest(season,ts.get(0).stadiumId(),List.of(),List.of()))).hasMessage("赛程已生成，不能继续报名");
        schedules.generateIfEligible(season,"MANUAL");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);
    }

    @Test void concurrentGenerateLeavesOneCompleteBatch() throws Exception {
        long season=season("IT16C并发",4,LocalDate.of(2047,3,1),LocalDate.of(2047,5,1),time.now().minusDays(1));teams(season,4);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{Callable<Long> call=()->{gate.await(5,TimeUnit.SECONDS);return schedules.generateIfEligible(season,"MANUAL").getBatchId();};Future<Long>a=pool.submit(call),b=pool.submit(call);assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,season)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(12);
    }

    @Test void onlyEventAdminCanGenerateAndConfirm() throws Exception {
        long season=season("IT16C权限",4,LocalDate.of(2048,3,1),LocalDate.of(2048,5,1),time.now().plusDays(2));teams(season,2);
        String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_club','demo_event_admin','demo_admin')",hash);
        for(String phone:List.of("13800000001","13800000003","13800000002"))mvc.perform(post("/api/admin/seasons/{id}/schedule/generate",season).header("Authorization",bearer(loginByPhone(phone)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/seasons/{id}/schedule/generate",season).header("Authorization",bearer(loginByPhone("13800000005")))).andExpect(status().isConflict());
    }

    @Test void afterCommitFullEventAndSchedulerDeadlineScanGenerateAutomatically(){
        long full=season("IT16C满额事件",4,LocalDate.of(2049,3,1),LocalDate.of(2049,5,1),time.now().plusDays(2));teams(full,4);
        transactions.executeWithoutResult(status->publisher.publishEvent(new ScheduleEligibilityEvent(full)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?",Integer.class,full)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT trigger_type FROM season_schedule_batch WHERE season_id=?",String.class,full)).isEqualTo("FULL");
        long expired=season("IT16C截止扫描",4,LocalDate.of(2050,3,1),LocalDate.of(2050,5,1),time.now().minusMinutes(1));teams(expired,2);
        task.deadlineScan();
        assertThat(jdbc.queryForObject("SELECT trigger_type FROM season_schedule_batch WHERE season_id=?",String.class,expired)).isEqualTo("DEADLINE");
    }

    private void assertRoundRobin(long season,List<Team> teams,int rounds,int matches,int home,int away){
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT round_id) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(rounds);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=?",Integer.class,season)).isEqualTo(matches);
        for(Team t:teams){assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND (home_club_id=? OR away_club_id=?)",Integer.class,season,t.clubId(),t.clubId())).isEqualTo(home+away);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND home_club_id=?",Integer.class,season,t.clubId())).isEqualTo(home);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info WHERE season_id=? AND away_club_id=?",Integer.class,season,t.clubId())).isEqualTo(away);assertThat(jdbc.queryForObject("SELECT MAX(c) FROM (SELECT round_id,COUNT(*) c FROM match_info WHERE season_id=? AND (home_club_id=? OR away_club_id=?) GROUP BY round_id) x",Integer.class,season,t.clubId(),t.clubId())).isEqualTo(1);}
        assertThat(jdbc.queryForObject("SELECT MIN(gap_days) FROM (SELECT DATEDIFF(match_time,LAG(match_time) OVER(PARTITION BY club_id ORDER BY match_time)) gap_days FROM (SELECT home_club_id club_id,match_time FROM match_info WHERE season_id=? UNION ALL SELECT away_club_id,match_time FROM match_info WHERE season_id=?) z) q WHERE gap_days IS NOT NULL",Integer.class,season,season)).isGreaterThanOrEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT LEAST(home_club_id,away_club_id) a,GREATEST(home_club_id,away_club_id) b,COUNT(*) c,COUNT(DISTINCT home_club_id) h FROM match_info WHERE season_id=? GROUP BY a,b HAVING c<>2 OR h<>2) x",Integer.class,season)).isZero();
    }
    private void assertVenuesUseEnrollmentSnapshot(long season){assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_info m JOIN club_season_enrollment e ON e.season_id=m.season_id AND e.club_id=m.home_club_id WHERE m.season_id=? AND m.stadium_id<>e.stadium_id",Integer.class,season)).isZero();}
    private long season(String name,int max,LocalDate start,LocalDate end,LocalDateTime deadline){jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,?,'DRAFT')",name,start,end,deadline.minusMonths(2),deadline,max);return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?",Long.class,name);}
    private List<Team> teams(long season,int n){List<Team> out=new ArrayList<>();for(int i=1;i<=n;i++){String suffix=season+"-"+i;jdbc.update("INSERT INTO stadium_info(stadium_name,city,address,capacity,stadium_status) VALUES(?,?,?,1000,'ACTIVE')","IT16C场馆"+suffix,"测试城","测试路"+i);long stadium=jdbc.queryForObject("SELECT stadium_id FROM stadium_info WHERE stadium_name=?",Long.class,"IT16C场馆"+suffix);jdbc.update("INSERT INTO club_info(club_name,home_city,home_stadium_id,club_status) VALUES(?,?,?,'ACTIVE')","IT16C俱乐部"+suffix,"测试城",stadium);long club=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name=?",Long.class,"IT16C俱乐部"+suffix);jdbc.update("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(?,?,?,'SUBMITTED',?)",season,club,stadium,time.now());out.add(new Team(club,stadium));}return out;}
    private long eventAdminId(){return jdbc.queryForObject("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE r.role_code='EVENT_ADMIN' ORDER BY u.user_id LIMIT 1",Long.class);}
    private String loginByPhone(String phone)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("phone",phone,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private static String bearer(String token){return "Bearer "+token;}
    private void cleanup(){jdbc.update("DELETE sm FROM season_schedule_match sm JOIN match_info m ON m.match_id=sm.match_id JOIN season_info s ON s.season_id=m.season_id WHERE s.season_name LIKE 'IT16C%'");jdbc.update("DELETE FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16C%')");jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'IT16C%'");jdbc.update("DELETE FROM club_info WHERE club_name LIKE 'IT16C%'");jdbc.update("DELETE FROM stadium_info WHERE stadium_name LIKE 'IT16C%'");}
    private record Team(long clubId,long stadiumId){}
}
