package com.example.leagueticket;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class CheckinIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder; @Autowired CheckinService checkinService; @Autowired RefundService refundService;
    long matchA,matchB,otherHomeMatch,zoneA,zoneB,userId,checkerId,adminId,checkerClubId;
    String userToken,checkerToken,adminToken,clubToken;AuthenticatedUser checker,admin;

    @BeforeEach void setup()throws Exception{
        cleanup();String hash=encoder.encode("123456");jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_admin','demo_checker','demo_club')",hash);
        userId=id("SELECT user_id FROM sys_user WHERE username='demo_user'");checkerId=id("SELECT user_id FROM sys_user WHERE username='demo_checker'");adminId=id("SELECT user_id FROM sys_user WHERE username='demo_admin'");checkerClubId=id("SELECT club_id FROM sys_user WHERE user_id="+checkerId);
        matchA=id("SELECT match_id FROM match_info WHERE home_club_id="+checkerClubId+" AND match_status='PUBLISHED' ORDER BY match_time LIMIT 1");
        matchB=id("SELECT match_id FROM match_info WHERE home_club_id="+checkerClubId+" AND match_status='PUBLISHED' AND match_id!="+matchA+" ORDER BY match_time LIMIT 1");
        otherHomeMatch=id("SELECT match_id FROM match_info WHERE home_club_id!="+checkerClubId+" AND match_status='PUBLISHED' ORDER BY match_time LIMIT 1");
        long stadium=id("SELECT stadium_id FROM match_info WHERE match_id="+matchA);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT13','IT13检票区',113,'ACTIVE')",stadium);long staticZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13'");
        for(int row=1;row<=3;row++)for(int seat=1;seat<=8;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,?,?,?,?,0,'ACTIVE')",stadium,staticZone,row+"排",row,seat+"座",seat);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT13检票区',88.00,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 12 HOUR))",matchA,staticZone,adminId);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT13检票区',98.00,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 12 HOUR))",matchB,staticZone,adminId);
        zoneA=id("SELECT match_zone_id FROM match_ticket_zone WHERE match_id="+matchA+" AND stadium_zone_id="+staticZone);zoneB=id("SELECT match_zone_id FROM match_ticket_zone WHERE match_id="+matchB+" AND stadium_zone_id="+staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",matchA,zoneA,staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",matchB,zoneB,staticZone);
        userToken=login("demo_user");checkerToken=login("demo_checker");adminToken=login("demo_admin");clubToken=login("demo_club");
        checker=principal(checkerId,"CHECKER",checkerClubId);admin=principal(adminId,"ADMIN",null);
    }
    @AfterEach void after(){cleanup();}

    @Test void normalDuplicateAndMissingCodeKeepOrderAndInventory()throws Exception{
        long order=paid(zoneA,1),ticket=id("SELECT ticket_id FROM e_ticket WHERE order_id="+order);String code=text("SELECT ticket_code FROM e_ticket WHERE ticket_id="+ticket);
        String first=check(matchA,code,checkerToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.checkResult").value("SUCCESS")).andReturn().getResponse().getContentAsString();LocalDateTime used=jdbc.queryForObject("SELECT used_at FROM e_ticket WHERE ticket_id=?",LocalDateTime.class,ticket);
        check(matchA,code,checkerToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.checkResult").value("TICKET_USED"));assertThat(jdbc.queryForObject("SELECT used_at FROM e_ticket WHERE ticket_id=?",LocalDateTime.class,ticket)).isEqualTo(used);
        check(matchA,"IT13-NOT-FOUND",checkerToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.checkResult").value("CODE_NOT_FOUND"));
        assertThat(count("SELECT COUNT(*) FROM checkin_record WHERE ticket_id=? AND check_result='SUCCESS'",ticket)).isEqualTo(1);assertThat(count("SELECT COUNT(*) FROM checkin_record WHERE ticket_id=? AND check_result='TICKET_USED'",ticket)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkin_record WHERE scanned_ticket_code='IT13-NOT-FOUND' AND ticket_id IS NULL",Integer.class)).isEqualTo(1);
        assertThat(text("SELECT order_status FROM ticket_order WHERE order_id="+order)).isEqualTo("PAID");assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='SOLD'",order)).isEqualTo(1);assertThat(json.readTree(first).path("data").path("checkedAt").asText()).isNotBlank();
    }

    @Test void wrongMatchRefundedVoidAndInvalidOrderProduceExactResults()throws Exception{
        long wrongOrder=paid(zoneA,1);String wrongCode=code(wrongOrder);check(matchB,wrongCode,checkerToken).andExpect(jsonPath("$.data.checkResult").value("WRONG_MATCH"));assertThat(text("SELECT ticket_status FROM e_ticket WHERE order_id="+wrongOrder)).isEqualTo("UNUSED");
        long refundedOrder=paid(zoneA,1);RefundResponse refund=refundService.apply(userId,refundedOrder,new RefundApplyRequest("IT13退票"));refundService.approve(adminId,refund.refundId(),new RefundAuditRequest("通过"));check(matchA,code(refundedOrder),checkerToken).andExpect(jsonPath("$.data.checkResult").value("TICKET_REFUNDED"));
        long voidOrder=paid(zoneA,1);jdbc.update("UPDATE e_ticket SET ticket_status='VOID',used_at=NULL WHERE order_id=?",voidOrder);check(matchA,code(voidOrder),checkerToken).andExpect(jsonPath("$.data.checkResult").value("TICKET_VOID"));
        long invalidOrder=paid(zoneA,1);jdbc.update("UPDATE ticket_order SET order_status='REFUND_PENDING' WHERE order_id=?",invalidOrder);check(matchA,code(invalidOrder),checkerToken).andExpect(jsonPath("$.data.checkResult").value("ORDER_INVALID"));
    }

    @Test void checkerScopeAdminOverrideRolesAndRecordQueriesWork()throws Exception{
        mvc.perform(get("/api/checker/matches").header("Authorization",bearer(checkerToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].homeClubId").value(checkerClubId));
        check(otherHomeMatch,"IT13-SCOPE",checkerToken).andExpect(status().isForbidden());check(otherHomeMatch,"IT13-ADMIN-MISSING",adminToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.checkResult").value("CODE_NOT_FOUND"));
        check(matchA,"IT13-CHECKER-MISSING",checkerToken).andExpect(status().isOk());
        mvc.perform(get("/api/checker/checkins").header("Authorization",bearer(checkerToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/checkins?checkResult=CODE_NOT_FOUND").header("Authorization",bearer(adminToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/api/checker/checkins").header("Authorization",bearer(userToken))).andExpect(status().isForbidden());check(matchA,"IT13-USER",userToken).andExpect(status().isForbidden());check(matchA,"IT13-CLUB",clubToken).andExpect(status().isForbidden());
    }

    @Test void concurrentScanHasExactlyOneSuccessAndOneUsed()throws Exception{
        long order=paid(zoneA,1),ticket=id("SELECT ticket_id FROM e_ticket WHERE order_id="+order);String code=code(order);List<CheckinResponse> results=runPair(()->checkinService.checkin(checker,matchA,new CheckinRequest(code)),()->checkinService.checkin(checker,matchA,new CheckinRequest(code)));
        assertThat(results).extracting(CheckinResponse::checkResult).containsExactlyInAnyOrder("SUCCESS","TICKET_USED");assertThat(count("SELECT COUNT(*) FROM checkin_record WHERE ticket_id=? AND check_result='SUCCESS'",ticket)).isEqualTo(1);assertThat(text("SELECT ticket_status FROM e_ticket WHERE ticket_id="+ticket)).isEqualTo("USED");
    }

    @Test void refundApprovalAndCheckinCannotProduceMixedState()throws Exception{
        long order=paid(zoneA,1);String code=code(order);RefundResponse refund=refundService.apply(userId,order,new RefundApplyRequest("并发退票"));List<Object> results=runObjects(()->refundService.approve(adminId,refund.refundId(),new RefundAuditRequest("通过")),()->checkinService.checkin(checker,matchA,new CheckinRequest(code)));
        assertThat(text("SELECT order_status FROM ticket_order WHERE order_id="+order)).isEqualTo("REFUNDED");assertThat(text("SELECT ticket_status FROM e_ticket WHERE order_id="+order)).isEqualTo("REFUNDED");assertThat(results.stream().filter(CheckinResponse.class::isInstance).map(CheckinResponse.class::cast).findFirst().orElseThrow().checkResult()).isIn("ORDER_INVALID","TICKET_REFUNDED");assertThat(count("SELECT COUNT(*) FROM checkin_record WHERE check_result='SUCCESS' AND ticket_id=(SELECT ticket_id FROM e_ticket WHERE order_id=?)",order)).isZero();
    }

    private long paid(long zone,int n)throws Exception{String body=mvc.perform(post("/api/orders").header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zone,"ticketCount",n)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();long order=json.readTree(body).path("data").path("order").path("orderId").asLong();mvc.perform(post("/api/orders/{id}/pay",order).header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON).content("{\"payMethod\":\"SIMULATED\",\"simulateResult\":\"SUCCESS\"}")).andExpect(status().isOk());return order;}
    private ResultActions check(long match,String code,String token)throws Exception{return mvc.perform(post("/api/checker/matches/{id}/checkin",match).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("ticketCode",code))));}
    private String code(long order){return text("SELECT ticket_code FROM e_ticket WHERE order_id="+order);}
    private String login(String username)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",username,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private AuthenticatedUser principal(long id,String role,Long club){return new AuthenticatedUser(id,"it13",role,role,club,List.of(),List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
    private <T> List<T> runPair(Callable<T>a,Callable<T>b)throws Exception{ExecutorService p=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);try{Future<T>x=p.submit(()->{barrier.await();return a.call();}),y=p.submit(()->{barrier.await();return b.call();});return List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS));}finally{p.shutdownNow();}}
    private List<Object> runObjects(Callable<?>a,Callable<?>b)throws Exception{return new ArrayList<>(runPair(()->(Object)a.call(),()->(Object)b.call()));}
    private String bearer(String t){return "Bearer "+t;}private long id(String sql){return jdbc.queryForObject(sql,Long.class);}private String text(String sql){return jdbc.queryForObject(sql,String.class);}private int count(String sql,long id){return jdbc.queryForObject(sql,Integer.class,id);}
    private void cleanup(){
        jdbc.update("DELETE FROM checkin_record WHERE scanned_ticket_code LIKE 'IT13%' OR ticket_id IN (SELECT ticket_id FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13'))))");
        jdbc.update("DELETE FROM refund_apply WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')))");jdbc.update("DELETE FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')))");jdbc.update("DELETE FROM payment_record WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')))");jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')))");jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13'))");jdbc.update("DELETE FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13'))");jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')");jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT13')");jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT13'");
    }
}
