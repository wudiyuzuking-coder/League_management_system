package com.example.leagueticket;

import com.example.leagueticket.dto.EnrollmentPlayerRequest;
import com.example.leagueticket.dto.EnrollmentRequest;
import com.example.leagueticket.service.ClubSeasonEnrollmentService;
import com.example.leagueticket.service.SystemTimeService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class ClubSeasonEnrollmentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder; @Autowired ClubSeasonEnrollmentService service; @Autowired SystemTimeService time;
    String clubToken,eventToken,userToken; long clubA,clubB,clubC;

    @BeforeEach void setup() throws Exception {
        cleanup();jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        List<Long> clubs=jdbc.queryForList("SELECT club_id FROM club_info WHERE home_stadium_id IS NOT NULL AND club_status='ACTIVE' ORDER BY club_id LIMIT 3",Long.class);
        clubA=clubs.get(0);clubB=clubs.get(1);clubC=clubs.get(2);ensureRoster(clubA,60);ensureRoster(clubB,40);ensureRoster(clubC,20);
        jdbc.update("UPDATE sys_user SET password_hash=?,club_id=?,user_status='ENABLED' WHERE username='demo_club'",encoder.encode("123456"),clubA);
        clubToken=loginByPhone("13800000003");eventToken=loginByPhone("13800000005");userToken=loginByPhone("13800000001");
    }
    @AfterEach void tearDown(){jdbc.update("UPDATE sys_config SET config_value='0' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");cleanup();}

    @Test void submitListDetailDuplicateAndEventAdminReadOnly() throws Exception {
        long season=season("IT16B报名主流程",time.now().toLocalDate().plusDays(60),time.now().toLocalDate().plusDays(120),4,-1,30);
        mvc.perform(get("/api/club/enrollments/available-seasons").header("Authorization",bearer(clubToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].seasonId",hasItem((int)season)));
        JsonNode created=response(mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(payload(clubA,season)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.playerCount").value(11)).andExpect(jsonPath("$.data.coachCount").value(1))
                .andExpect(jsonPath("$.data.players[0].age",notNullValue())).andReturn().getResponse().getContentAsString()).path("data");
        long id=created.path("enrollmentId").asLong();
        mvc.perform(get("/api/club/enrollments/{id}",id).header("Authorization",bearer(clubToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.players",hasSize(11)));
        mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(payload(clubA,season))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("该俱乐部已报名此赛季"));
        mvc.perform(get("/api/admin/enrollments").param("seasonId",Long.toString(season)).header("Authorization",bearer(eventToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/enrollments/{id}",id).header("Authorization",bearer(eventToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value((int)clubA));
        mvc.perform(get("/api/admin/enrollments").header("Authorization",bearer(userToken))).andExpect(status().isForbidden());
    }

    @Test void timeWindowUsesSystemTimeAndDeadlineIsExclusive() throws Exception {
        LocalDateTime now=time.now().truncatedTo(ChronoUnit.SECONDS);long season=seasonAt("IT16B时间窗口",now.toLocalDate().plusDays(70),now.toLocalDate().plusDays(120),4,now.plusDays(1),now.plusDays(3));
        available(season).andExpect(jsonPath("$.data[*].seasonId",not(hasItem((int)season))));
        setSystemTime(now.plusDays(2));available(season).andExpect(jsonPath("$.data[*].seasonId",hasItem((int)season)));
        setSystemTime(now.plusDays(3).plusSeconds(2));available(season).andExpect(jsonPath("$.data[*].seasonId",not(hasItem((int)season))));
        mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(payload(clubA,season))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("赛季报名已截止"));
    }

    @Test void overlappingSeasonAndForeignRosterAreRejectedWithoutResidue() throws Exception {
        LocalDate base=time.now().toLocalDate().plusDays(60);long a=season("IT16B冲突A",base,base.plusDays(90),4,-1,30);service.submit(clubA,request(clubA,a));
        long b=season("IT16B冲突B",base.plusDays(30),base.plusDays(120),4,-1,30);
        mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(payload(clubA,b))).andExpect(status().isConflict()).andExpect(jsonPath("$.message",containsString("时间冲突")));
        long c=season("IT16B越权阵容",base.plusDays(200),base.plusDays(260),4,-1,30);Map<String,Object> bad=payloadMap(clubA,c);((List<Map<String,Object>>)bad.get("players")).set(0,Map.of("playerId",players(clubB).get(0),"lineupRole","STARTER"));
        mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bad))).andExpect(status().isForbidden());
        assertThat(countEnrollments(c)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_enrollment_player ep JOIN club_season_enrollment e ON e.enrollment_id=ep.enrollment_id WHERE e.season_id=?",Integer.class,c)).isZero();
    }

    @Test void minimumRosterCoachAndHomeStadiumAreEnforced() throws Exception {
        long season=season("IT16B资格",time.now().toLocalDate().plusDays(60),time.now().toLocalDate().plusDays(100),4,-1,30);Map<String,Object> ten=payloadMap(clubA,season);((List<?>)ten.get("players")).remove(0);
        mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ten))).andExpect(status().isBadRequest());
        Map<String,Object> noCoach=payloadMap(clubA,season);noCoach.put("coachIds",List.of());mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(noCoach))).andExpect(status().isBadRequest());
        Map<String,Object> otherStadium=payloadMap(clubA,season);otherStadium.put("stadiumId",stadium(clubB));mvc.perform(post("/api/club/enrollments").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(otherStadium))).andExpect(status().isForbidden());
        assertThat(countEnrollments(season)).isZero();
    }

    @Test void seasonApiRequiresAndValidatesRegistrationConfiguration() throws Exception {
        Map<String,Object> valid=new LinkedHashMap<>();valid.put("seasonName","IT16B接口赛季");valid.put("startDate","2038-03-01");valid.put("endDate","2038-12-01");valid.put("registrationStartTime","2038-01-01T00:00:00");valid.put("registrationDeadline","2038-02-22T00:00:00");valid.put("maxClubs",20);valid.put("description","test");
        mvc.perform(post("/api/admin/seasons").header("Authorization",bearer(eventToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid))).andExpect(status().isOk());
        valid.put("seasonName","IT16B错误间隔");valid.put("registrationStartTime","2038-02-15T00:00:00");mvc.perform(post("/api/admin/seasons").header("Authorization",bearer(eventToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid))).andExpect(status().isBadRequest());
        valid.put("seasonName","IT16B错误截止");valid.put("registrationStartTime","2038-01-01T00:00:00");valid.put("registrationDeadline","2038-02-23T00:00:00");mvc.perform(post("/api/admin/seasons").header("Authorization",bearer(eventToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid))).andExpect(status().isBadRequest());
    }

    @Test void concurrentDifferentClubsCannotExceedLastSlot() throws Exception {
        long season=season("IT16B最后名额",time.now().toLocalDate().plusDays(60),time.now().toLocalDate().plusDays(100),1,-1,30);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{Future<Boolean>a=pool.submit(()->submitAtGate(gate,clubA,season));Future<Boolean>b=pool.submit(()->submitAtGate(gate,clubB,season));assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}finally{pool.shutdownNow();}
        assertThat(countEnrollments(season)).isEqualTo(1);
        long loser=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_id IN (?,?) AND club_id NOT IN (SELECT club_id FROM club_season_enrollment WHERE season_id=?) LIMIT 1",Long.class,clubA,clubB,season);
        assertThat(service.availableSeasons(loser).stream().noneMatch(x->x.getSeasonId().equals(season))).isTrue();
        try{service.submit(loser,request(loser,season));Assertions.fail("capacity should reject");}catch(Exception expected){assertThat(expected.getMessage()).isEqualTo("赛季报名名额已满");}
    }

    @Test void concurrentDuplicateEnrollmentLeavesOneRecord() throws Exception {
        long season=season("IT16B并发重复",time.now().toLocalDate().plusDays(60),time.now().toLocalDate().plusDays(100),4,-1,30);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier gate=new CyclicBarrier(2);
        try{Future<Boolean>a=pool.submit(()->submitAtGate(gate,clubA,season));Future<Boolean>b=pool.submit(()->submitAtGate(gate,clubA,season));assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}finally{pool.shutdownNow();}
        assertThat(countEnrollments(season)).isEqualTo(1);
    }

    private boolean submitAtGate(CyclicBarrier gate,long club,long season){try{gate.await(5,TimeUnit.SECONDS);service.submit(club,request(club,season));return true;}catch(Exception e){return false;}}
    private org.springframework.test.web.servlet.ResultActions available(long season)throws Exception{return mvc.perform(get("/api/club/enrollments/available-seasons").header("Authorization",bearer(clubToken))).andExpect(status().isOk());}
    private void setSystemTime(LocalDateTime value)throws Exception{mvc.perform(put("/api/system-time").header("Authorization",bearer(clubToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("targetTime",value)))).andExpect(status().isOk());}
    private long season(String name,LocalDate start,LocalDate end,int max,long regStartDays,long deadlineDays){LocalDateTime now=time.now();return seasonAt(name,start,end,max,now.plusDays(regStartDays),now.plusDays(deadlineDays));}
    private long seasonAt(String name,LocalDate start,LocalDate end,int max,LocalDateTime registrationStart,LocalDateTime deadline){jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,?,'DRAFT')",name,start,end,registrationStart,deadline,max);return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?",Long.class,name);}
    private EnrollmentRequest request(long club,long season){List<Long> ps=players(club);List<EnrollmentPlayerRequest> items=new ArrayList<>();for(int i=0;i<11;i++)items.add(new EnrollmentPlayerRequest(ps.get(i),i<5?"STARTER":"SUBSTITUTE"));return new EnrollmentRequest(season,stadium(club),items,List.of(coach(club)));}
    private String payload(long club,long season)throws Exception{return json.writeValueAsString(payloadMap(club,season));}
    private Map<String,Object> payloadMap(long club,long season){EnrollmentRequest r=request(club,season);Map<String,Object> m=new LinkedHashMap<>();m.put("seasonId",season);m.put("stadiumId",r.stadiumId());m.put("players",new ArrayList<>(r.players().stream().map(x->new LinkedHashMap<String,Object>(Map.of("playerId",x.playerId(),"lineupRole",x.lineupRole()))).toList()));m.put("coachIds",r.coachIds());return m;}
    private List<Long> players(long club){return jdbc.queryForList("SELECT player_id FROM player_info WHERE club_id=? AND player_status='ACTIVE' ORDER BY shirt_no LIMIT 11",Long.class,club);}
    private long coach(long club){return jdbc.queryForObject("SELECT coach_id FROM coach_info WHERE club_id=? AND coach_status='ACTIVE' ORDER BY coach_id LIMIT 1",Long.class,club);}
    private long stadium(long club){return jdbc.queryForObject("SELECT home_stadium_id FROM club_info WHERE club_id=?",Long.class,club);}
    private void ensureRoster(long club,int first){jdbc.update("UPDATE player_info SET birth_date='2000-01-01' WHERE club_id=? AND birth_date IS NULL",club);for(int i=0;i<11;i++){int shirt=first+i;jdbc.update("INSERT INTO player_info(club_id,player_name,shirt_no,position,nationality,birth_date,player_status) VALUES(?,?,?,?,?,'2000-01-01','ACTIVE') ON DUPLICATE KEY UPDATE player_status='ACTIVE',birth_date='2000-01-01'",club,"IT16B球员"+club+"-"+shirt,shirt,i==0?"GOALKEEPER":i<5?"DEFENDER":i<8?"MIDFIELDER":"FORWARD","中国");}jdbc.update("INSERT INTO coach_info(club_id,coach_name,title,coach_status) SELECT ?,?,'HEAD_COACH','ACTIVE' WHERE NOT EXISTS(SELECT 1 FROM coach_info WHERE club_id=? AND coach_name=?)",club,"IT16B教练"+club,club,"IT16B教练"+club);}
    private int countEnrollments(long season){return jdbc.queryForObject("SELECT COUNT(*) FROM club_season_enrollment WHERE season_id=?",Integer.class,season);}
    private String loginByPhone(String phone)throws Exception{return response(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("phone",phone,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("token").asText();}
    private JsonNode response(String value)throws Exception{return json.readTree(value);} private static String bearer(String token){return "Bearer "+token;}
    private void cleanup(){jdbc.update("DELETE FROM club_season_enrollment_player WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16B%'))");jdbc.update("DELETE FROM club_season_enrollment_coach WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16B%'))");jdbc.update("DELETE FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT16B%')");jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'IT16B%'");}
}
