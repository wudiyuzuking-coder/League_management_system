package com.example.leagueticket;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.mapper.ETicketMapper;
import com.example.leagueticket.service.RefundService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class RefundIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired RefundService service;@Autowired ETicketMapper ticketMapper;@Autowired PasswordEncoder passwordEncoder;
    long zoneId,userId,otherId,adminId,matchId;String user,other,admin;

    @BeforeEach void setup()throws Exception{
        cleanup();long role=id("SELECT role_id FROM sys_role WHERE role_code='USER'"),stadium;String hash=passwordEncoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=? WHERE username IN ('demo_user','demo_admin')",hash);
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,role_id,user_status) VALUES('it12_user2','13900001212',?,'IT12用户2',?,'ENABLED')",hash,role);
        userId=id("SELECT user_id FROM sys_user WHERE username='demo_user'");otherId=id("SELECT user_id FROM sys_user WHERE username='it12_user2'");adminId=id("SELECT user_id FROM sys_user WHERE username='demo_admin'");
        matchId=id("SELECT match_id FROM match_info WHERE match_status='PUBLISHED' AND match_time>DATE_ADD(NOW(),INTERVAL 2 DAY) ORDER BY match_id LIMIT 1");stadium=id("SELECT stadium_id FROM match_info WHERE match_id="+matchId);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT12','IT12退票区',112,'ACTIVE')",stadium);
        long staticZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12'");for(int row=1;row<=3;row++)for(int seat=1;seat<=8;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,?,?,?,?,0,'ACTIVE')",stadium,staticZone,row+"排",row,seat+"座",seat);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT12退票区',75.00,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 12 HOUR))",matchId,staticZone,adminId);zoneId=id("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id="+staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",matchId,zoneId,staticZone);
        user=login("demo_user");other=login("it12_user2");admin=login("demo_admin");
    }
    @AfterEach void after(){cleanup();}

    @Test void applyAndApproveRestoresInventoryAndKeepsHistory()throws Exception{
        long order=paid(2,user);int maxBefore=maxContinuous();
        String body=apply(order,user).andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value("PENDING")).andReturn().getResponse().getContentAsString();long refund=json.readTree(body).path("data").path("refundId").asLong();
        assertThat(orderStatus(order)).isEqualTo("REFUND_PENDING");assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='PAID'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=? AND ticket_status='UNUSED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='SOLD'",order)).isEqualTo(2);
        mvc.perform(post("/api/admin/refunds/{id}/approve",refund).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"auditReason\":\"符合退票规则\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value("APPROVED"));
        assertThat(orderStatus(order)).isEqualTo("REFUNDED");assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='REFUNDED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=? AND ticket_status='REFUNDED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='AVAILABLE' AND i.lock_order_id IS NULL AND i.locked_at IS NULL AND i.lock_expire_time IS NULL",order)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT refund_amount FROM refund_apply WHERE refund_id=?",java.math.BigDecimal.class,refund)).isEqualByComparingTo("150.00");assertThat(maxContinuous()).isEqualTo(maxBefore);
        mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",zoneId).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":2}" )).andExpect(status().isOk());
        mvc.perform(get("/api/refunds/{id}",refund).header("Authorization",bearer(other))).andExpect(status().isNotFound());mvc.perform(post("/api/orders/{id}/refund",order).header("Authorization",bearer(other)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"越权\"}" )).andExpect(status().isForbidden());
    }

    @Test void rejectionRestoresOnlyOrder()throws Exception{
        long order=paid(2,user);long refund=refundId(apply(order,user));mvc.perform(post("/api/admin/refunds/{id}/reject",refund).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"auditReason\":\"不符合条件\"}" )).andExpect(status().isOk());
        assertThat(orderStatus(order)).isEqualTo("PAID");assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='PAID'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=? AND ticket_status='UNUSED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='SOLD'",order)).isEqualTo(2);
        mvc.perform(post("/api/admin/refunds/{id}/reject",refund).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isOk());mvc.perform(post("/api/admin/refunds/{id}/approve",refund).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isConflict());
    }

    @Test void deadlineUsedTicketAndInvalidOrderAreRejected()throws Exception{
        long order=paid(1,user);LocalDateTime original=jdbc.queryForObject("SELECT match_time FROM match_info WHERE match_id=?",LocalDateTime.class,matchId);try{jdbc.update("UPDATE match_info SET match_time=DATE_ADD(NOW(),INTERVAL 24 HOUR) WHERE match_id=?",matchId);apply(order,user).andExpect(status().isConflict());}finally{jdbc.update("UPDATE match_info SET match_time=? WHERE match_id=?",original,matchId);}
        long ticket=id("SELECT ticket_id FROM e_ticket WHERE order_id="+order);jdbc.update("UPDATE e_ticket SET ticket_status='USED',used_at=NOW() WHERE ticket_id=?",ticket);apply(order,user).andExpect(status().isConflict());
        long pending=create(1,user);apply(pending,user).andExpect(status().isConflict());mvc.perform(post("/api/orders/{id}/cancel",pending).header("Authorization",bearer(user))).andExpect(status().isOk());apply(pending,user).andExpect(status().isConflict());
    }

    @Test void duplicateApplyDoubleApproveAndApproveRejectRemainConsistent()throws Exception{
        long order=paid(2,user);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier b=new CyclicBarrier(2);try{Future<?>a=pool.submit(()->attempt(()->service.apply(userId,order,new RefundApplyRequest("并发申请")),b)),c=pool.submit(()->attempt(()->service.apply(userId,order,new RefundApplyRequest("并发申请")),b));a.get(15,TimeUnit.SECONDS);c.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}assertThat(count("SELECT COUNT(*) FROM refund_apply WHERE order_id=?",order)).isEqualTo(1);long refund=id("SELECT refund_id FROM refund_apply WHERE order_id="+order);
        runPair(()->service.approve(adminId,refund,new RefundAuditRequest("通过")),()->service.approve(adminId,refund,new RefundAuditRequest("通过")));assertThat(orderStatus(order)).isEqualTo("REFUNDED");assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=? AND ticket_status='REFUNDED'",order)).isEqualTo(2);
        long order2=paid(2,user);long refund2=refundId(apply(order2,user));runPair(()->service.approve(adminId,refund2,new RefundAuditRequest("通过")),()->service.reject(adminId,refund2,new RefundAuditRequest("驳回")));String rs=jdbc.queryForObject("SELECT refund_status FROM refund_apply WHERE refund_id=?",String.class,refund2),os=orderStatus(order2);assertThat(rs+"|"+os).isIn("APPROVED|REFUNDED","REJECTED|PAID");
    }

    @Test void approvalAndUsedTransitionCannotCreateMixedState()throws Exception{
        long order=paid(1,user),refund=refundId(apply(order,user)),ticket=id("SELECT ticket_id FROM e_ticket WHERE order_id="+order);runPair(()->service.approve(adminId,refund,new RefundAuditRequest("通过")),()->ticketMapper.markUsedForTest(ticket,LocalDateTime.now()));String os=orderStatus(order),ts=jdbc.queryForObject("SELECT ticket_status FROM e_ticket WHERE ticket_id=?",String.class,ticket);assertThat(os+"|"+ts).isIn("REFUNDED|REFUNDED","REFUND_PENDING|USED");
    }

    private long paid(int n,String auth)throws Exception{long id=create(n,auth);mvc.perform(post("/api/orders/{id}/pay",id).header("Authorization",bearer(auth)).contentType(MediaType.APPLICATION_JSON).content("{\"payMethod\":\"SIMULATED\",\"simulateResult\":\"SUCCESS\"}" )).andExpect(status().isOk());return id;}
    private long create(int n,String auth)throws Exception{String body=mvc.perform(post("/api/orders").header("Authorization",bearer(auth)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zoneId,"ticketCount",n)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("order").path("orderId").asLong();}
    private ResultActions apply(long order,String auth)throws Exception{return mvc.perform(post("/api/orders/{id}/refund",order).header("Authorization",bearer(auth)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"临时无法观赛\"}"));}private long refundId(ResultActions a)throws Exception{return json.readTree(a.andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("refundId").asLong();}
    private void runPair(Runnable a,Runnable c)throws Exception{ExecutorService p=Executors.newFixedThreadPool(2);CyclicBarrier b=new CyclicBarrier(2);try{Future<?>x=p.submit(()->attempt(a,b)),y=p.submit(()->attempt(c,b));x.get(15,TimeUnit.SECONDS);y.get(15,TimeUnit.SECONDS);}finally{p.shutdownNow();}}
    private void attempt(Runnable action,CyclicBarrier b){try{b.await(5,TimeUnit.SECONDS);action.run();}catch(RuntimeException ignored){}catch(Exception e){throw new RuntimeException(e);}}
    private int maxContinuous()throws Exception{return json.readTree(mvc.perform(get("/api/match-ticket-zones/{id}/availability",zoneId).header("Authorization",bearer(user))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("maxContinuousCount").asInt();}
    private String login(String username)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",username,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private String bearer(String t){return "Bearer "+t;}private long id(String sql){return jdbc.queryForObject(sql,Long.class);}private int count(String sql,long id){return jdbc.queryForObject(sql,Integer.class,id);}private String orderStatus(long id){return jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?",String.class,id);}
    private void cleanup(){jdbc.update("DELETE FROM refund_apply WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')))");jdbc.update("DELETE FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')))");jdbc.update("DELETE FROM payment_record WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')))");jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')))");jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12'))");jdbc.update("DELETE FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12'))");jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')");jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT12')");jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT12'");jdbc.update("DELETE FROM sys_user WHERE username='it12_user2'");}
}
