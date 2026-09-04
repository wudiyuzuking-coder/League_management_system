package com.example.leagueticket;

import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.OrderService;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class SystemTimeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemTimeService systemTimeService;
    @Autowired OrderService orderService;

    private String userToken,clubToken,eventAdminToken,adminToken;
    private Long matchId,zoneId;
    private LocalDateTime originalMatchTime;

    @BeforeEach
    void setup() throws Exception {
        jdbc.update("INSERT INTO sys_config(config_key,config_value,value_type,description,config_status) VALUES('SYSTEM_TIME_OFFSET_SECONDS','0','INTEGER','test','ENABLED') ON DUPLICATE KEY UPDATE config_value='0',config_status='ENABLED'");
        jdbc.update("DELETE FROM operation_log WHERE module_name='SYSTEM_TIME'");
        String hash=passwordEncoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_club','demo_event_admin','demo_admin')",hash);
        Long clubId=jdbc.queryForObject("SELECT MIN(club_id) FROM club_info",Long.class);
        jdbc.update("UPDATE sys_user SET club_id=? WHERE username='demo_club'",clubId);
        userToken=loginByPhone("13800000001");clubToken=loginByPhone("13800000003");eventAdminToken=loginByPhone("13800000005");adminToken=loginByPhone("13800000002");
    }

    @AfterEach
    void cleanup(){
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        cleanupScenario();
        jdbc.update("DELETE FROM operation_log WHERE module_name='SYSTEM_TIME'");
    }

    @Test
    void defaultFuturePastResetAndTimeContinues() throws Exception {
        JsonNode initial=getTime(userToken);
        assertThat(initial.path("offsetSeconds").asLong()).isZero();
        assertClose(parse(initial,"systemTime"),parse(initial,"realTime"),2);

        LocalDateTime future=systemTimeService.realNow().plusDays(180).truncatedTo(ChronoUnit.SECONDS);
        JsonNode setFuture=response(setTime(userToken,future).andExpect(status().isOk())).path("data");
        assertClose(parse(setFuture,"systemTime"),future,2);
        LocalDateTime first=parse(getTime(userToken),"systemTime");
        Thread.sleep(1100);
        LocalDateTime second=parse(getTime(userToken),"systemTime");
        assertThat(second).isAfter(first);

        LocalDateTime past=systemTimeService.realNow().minusDays(90).truncatedTo(ChronoUnit.SECONDS);
        setTime(userToken,past).andExpect(status().isOk());
        assertClose(parse(getTime(userToken),"systemTime"),past,2);

        mvc.perform(post("/api/system-time/reset").header("Authorization",bearer(userToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.offsetSeconds").value(0));
        JsonNode reset=getTime(userToken);assertClose(parse(reset,"systemTime"),parse(reset,"realTime"),2);
    }

    @Test
    void allFourRolesCanAdjustAndAnonymousIsRejected() throws Exception {
        List<String> tokens=List.of(userToken,clubToken,eventAdminToken,adminToken);
        for(int i=0;i<tokens.size();i++)setTime(tokens.get(i),systemTimeService.realNow().plusDays(i+1)).andExpect(status().isOk());
        mvc.perform(get("/api/system-time")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/system-time").contentType(MediaType.APPLICATION_JSON).content("{\"targetTime\":\"2027-01-01T00:00:00\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/system-time/reset")).andExpect(status().isUnauthorized());
    }

    @Test
    void changesAndResetWriteReconstructableRealTimeLogs() throws Exception {
        LocalDateTime before=systemTimeService.realNow();
        setTime(userToken,before.plusDays(10)).andExpect(status().isOk());
        setTime(adminToken,before.minusDays(10)).andExpect(status().isOk());
        mvc.perform(post("/api/system-time/reset").header("Authorization",bearer(adminToken))).andExpect(status().isOk());
        List<String> descriptions=jdbc.queryForList("SELECT operation_description FROM operation_log WHERE module_name='SYSTEM_TIME' ORDER BY log_id",String.class);
        assertThat(descriptions).hasSize(3);
        assertThat(descriptions.get(0)).contains("username=demo_user","role=USER","beforeSystemTime=","afterSystemTime=","offsetSeconds=","realTime=");
        assertThat(descriptions.get(1)).contains("username=demo_admin","role=ADMIN");
        LocalDateTime min=jdbc.queryForObject("SELECT MIN(created_at) FROM operation_log WHERE module_name='SYSTEM_TIME'",LocalDateTime.class);
        assertThat(min).isAfterOrEqualTo(before.minusSeconds(2));
        assertThat(min).isBeforeOrEqualTo(systemTimeService.realNow().plusSeconds(2));
    }

    @Test
    void concurrentAdjustmentsKeepOneReadableOffset() throws Exception {
        long userId=id("SELECT user_id FROM sys_user WHERE username='demo_user'");
        long adminId=id("SELECT user_id FROM sys_user WHERE username='demo_admin'");
        AuthenticatedUser user=new AuthenticatedUser(userId,"demo_user","13800000001","演示普通用户",null,"USER",null,"ENABLED",List.of(),List.of());
        AuthenticatedUser admin=new AuthenticatedUser(adminId,"demo_admin","13800000002","演示管理员","SA0001","ADMIN",null,"ENABLED",List.of(),List.of());
        LocalDateTime real=systemTimeService.realNow();LocalDateTime a=real.plusDays(30),b=real.minusDays(30);
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
        try{
            Future<?> first=pool.submit(()->{await(barrier);systemTimeService.setCurrentSystemTime(a,user);});
            Future<?> second=pool.submit(()->{await(barrier);systemTimeService.setCurrentSystemTime(b,admin);});
            first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_config WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",Integer.class)).isEqualTo(1);
        long offset=Long.parseLong(jdbc.queryForObject("SELECT config_value FROM sys_config WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",String.class));
        assertThat(Math.abs(offset)).isBetween(Duration.ofDays(29).toSeconds(),Duration.ofDays(31).toSeconds());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operation_log WHERE module_name='SYSTEM_TIME'",Integer.class)).isEqualTo(2);
    }

    @Test
    void saleWindowAndOrderExpiryUseSystemTime() throws Exception {
        LocalDateTime businessNow=systemTimeService.realNow().plusDays(120).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime saleStart=businessNow.toLocalDate().atTime(20,0);
        setupTicketScenario(saleStart.plusDays(7),saleStart.plusHours(1));
        setTime(userToken,saleStart.minusSeconds(1)).andExpect(status().isOk());
        mvc.perform(get("/api/match-ticket-zones/{id}",zoneId).header("Authorization",bearer(userToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.saleAvailable").value(false));
        createOrder().andExpect(status().isConflict());

        setTime(userToken,saleStart).andExpect(status().isOk());
        mvc.perform(get("/api/match-ticket-zones/{id}",zoneId).header("Authorization",bearer(userToken))).andExpect(jsonPath("$.data.saleAvailable").value(true));
        JsonNode order=response(createOrder().andExpect(status().isOk())).path("data").path("order");
        long orderId=order.path("orderId").asLong();
        LocalDateTime expire=LocalDateTime.parse(order.path("expireTime").asText());
        assertClose(expire,saleStart.plusMinutes(15),2);

        setTime(userToken,expire.plusSeconds(1)).andExpect(status().isOk());
        assertThat(orderService.closeExpiredBatch()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?",String.class,orderId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='AVAILABLE'",Integer.class,zoneId)).isEqualTo(8);

        setTime(userToken,saleStart.plusHours(2)).andExpect(status().isOk());
        mvc.perform(get("/api/match-ticket-zones/{id}",zoneId).header("Authorization",bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.saleAvailable").value(false));
        createOrder().andExpect(status().isConflict());
    }

    @Test
    void refundDeadlineUsesSystemTime() throws Exception {
        LocalDateTime businessNow=systemTimeService.realNow().plusDays(150).truncatedTo(ChronoUnit.SECONDS);
        setupTicketScenario(businessNow.plusHours(48),businessNow.plusHours(1));
        setTime(userToken,businessNow).andExpect(status().isOk());
        long beforeOrder=paidOrder();long afterOrder=paidOrder();
        applyRefund(beforeOrder).andExpect(status().isOk());
        setTime(userToken,businessNow.plusHours(25)).andExpect(status().isOk());
        applyRefund(afterOrder).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("refund deadline has passed"));
    }

    private void setupTicketScenario(LocalDateTime matchTime,LocalDateTime saleEndTime){
        cleanupScenario();
        matchId=id("SELECT match_id FROM match_info WHERE match_status='PUBLISHED' ORDER BY match_id LIMIT 1");
        originalMatchTime=jdbc.queryForObject("SELECT match_time FROM match_info WHERE match_id=?",LocalDateTime.class,matchId);
        jdbc.update("UPDATE match_info SET match_time=? WHERE match_id=?",matchTime,matchId);
        long stadium=id("SELECT stadium_id FROM match_info WHERE match_id="+matchId),admin=id("SELECT user_id FROM sys_user WHERE username='demo_event_admin'");
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT16A','IT16A系统时间区',160,'ACTIVE')",stadium);
        long staticZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A'");
        for(int seat=1;seat<=8;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'1排',1,?,?,0,'ACTIVE')",stadium,staticZone,seat+"座",seat);
        LocalDateTime saleStart=matchTime.toLocalDate().minusDays(7).atTime(20,0);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT16A系统时间区',80,'ON_SALE',?,?)",matchId,staticZone,admin,saleStart,saleEndTime);
        zoneId=id("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id="+staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",matchId,zoneId,staticZone);
    }

    private ResultActions createOrder() throws Exception{return mvc.perform(post("/api/orders").header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zoneId,"ticketCount",1))));}
    private long paidOrder() throws Exception{JsonNode order=response(createOrder().andExpect(status().isOk())).path("data").path("order");long id=order.path("orderId").asLong();mvc.perform(post("/api/orders/{id}/pay",id).header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON).content("{\"payMethod\":\"SIMULATED\",\"simulateResult\":\"SUCCESS\"}")).andExpect(status().isOk());return id;}
    private ResultActions applyRefund(long orderId) throws Exception{return mvc.perform(post("/api/orders/{id}/refund",orderId).header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"IT16A时间测试\"}"));}
    private JsonNode getTime(String token) throws Exception{return json.readTree(mvc.perform(get("/api/system-time").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");}
    private ResultActions setTime(String token,LocalDateTime target) throws Exception{return mvc.perform(put("/api/system-time").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("targetTime",target.truncatedTo(ChronoUnit.SECONDS)))));}
    private String loginByPhone(String phone) throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private void cleanupScenario(){
        if(originalMatchTime!=null&&matchId!=null)jdbc.update("UPDATE match_info SET match_time=? WHERE match_id=?",originalMatchTime,matchId);
        jdbc.update("DELETE FROM refund_apply WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')))");
        jdbc.update("DELETE FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')))");
        jdbc.update("DELETE FROM payment_record WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')))");
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')))");
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A'))");
        jdbc.update("DELETE FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT16A')");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT16A'");
        matchId=null;zoneId=null;originalMatchTime=null;
    }
    private static LocalDateTime parse(JsonNode node,String field){return LocalDateTime.parse(node.path(field).asText());}
    private JsonNode response(ResultActions actions) throws Exception{return json.readTree(actions.andReturn().getResponse().getContentAsString());}
    private static void assertClose(LocalDateTime actual,LocalDateTime expected,long seconds){assertThat(Math.abs(Duration.between(expected,actual).toSeconds())).isLessThanOrEqualTo(seconds);}
    private static void await(CyclicBarrier barrier){try{barrier.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private static String bearer(String token){return "Bearer "+token;}

}
