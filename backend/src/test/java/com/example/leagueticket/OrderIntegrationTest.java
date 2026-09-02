package com.example.leagueticket;

import com.example.leagueticket.service.OrderService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class OrderIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired OrderService service;
    long zoneId,userAId,userBId;String userA,userB,club;

    @BeforeEach void setup()throws Exception{
        cleanup();
        long role=jdbc.queryForObject("SELECT role_id FROM sys_role WHERE role_code='USER'",Long.class);
        String hash=jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE username='demo_user'",String.class);
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,role_id,user_status) VALUES('it10_user2','13900001010',?,'IT10用户2',?,'ENABLED')",hash,role);
        userAId=id("SELECT user_id FROM sys_user WHERE username='demo_user'");userBId=id("SELECT user_id FROM sys_user WHERE username='it10_user2'");
        long match=id("SELECT match_id FROM match_info WHERE match_status='PUBLISHED' AND match_time>DATE_ADD(NOW(),INTERVAL 1 DAY) ORDER BY match_id LIMIT 1");
        long stadium=id("SELECT stadium_id FROM match_info WHERE match_id="+match),admin=id("SELECT user_id FROM sys_user WHERE username='demo_admin'");
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT10','IT10订单区',100,'ACTIVE')",stadium);
        long staticZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10'");
        for(int row=1;row<=2;row++)for(int seat=1;seat<=4;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,?, ?,?, ?,0,'ACTIVE')",stadium,staticZone,row+"排",row,seat+"座",seat);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT10订单区',88.50,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 12 HOUR))",match,staticZone,admin);
        zoneId=id("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id="+staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",match,zoneId,staticZone);
        userA=loginByPhone("13800000001");userB=loginByPhone("13900001010");club=loginByPhone("13800000003");
    }
    @AfterEach void after(){jdbc.execute("DROP TRIGGER IF EXISTS it10_fail_item");cleanup();}

    @Test void createsOneToFourTicketsWithSnapshotsAndRejectsInvalidCounts()throws Exception{
        for(int count=1;count<=4;count++){
            create(count,userA).andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(count));
            long order=id("SELECT MAX(order_id) FROM ticket_order WHERE user_id="+userAId);
            assertThat(jdbc.queryForObject("SELECT total_amount FROM ticket_order WHERE order_id=?",java.math.BigDecimal.class,order)).isEqualByComparingTo(java.math.BigDecimal.valueOf(88.5*count));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='LOCKED'",Integer.class,order)).isEqualTo(count);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=? AND inventory_status='LOCKED' AND locked_at IS NOT NULL AND lock_expire_time IS NOT NULL",Integer.class,order)).isEqualTo(count);
            assertThat(jdbc.queryForObject("SELECT TIMESTAMPDIFF(SECOND,expire_time,(SELECT MAX(lock_expire_time) FROM match_seat_inventory WHERE lock_order_id=?)) FROM ticket_order WHERE order_id=?",Integer.class,order,order)).isZero();
            cancel(order,userA).andExpect(status().isOk());
        }
        create(0,userA).andExpect(status().isBadRequest());create(5,userA).andExpect(status().isBadRequest());
        create(1,club).andExpect(status().isForbidden());
    }

    @Test void noContinuousSeatsAndItemFailureRollBackEveryStep()throws Exception{
        jdbc.update("UPDATE match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id SET i.inventory_status='DISABLED' WHERE i.match_zone_id=? AND s.seat_seq IN (2,4)",zoneId);
        long before=jdbc.queryForObject("SELECT COUNT(*) FROM ticket_order",Long.class);
        create(4,userA).andExpect(status().isConflict());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_order",Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='LOCKED'",Integer.class,zoneId)).isZero();
        jdbc.update("UPDATE match_seat_inventory SET inventory_status='AVAILABLE' WHERE match_zone_id=?",zoneId);
        jdbc.execute("CREATE TRIGGER it10_fail_item BEFORE INSERT ON order_item FOR EACH ROW SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO=1062, MESSAGE_TEXT='IT10 forced duplicate item'");
        create(2,userA).andExpect(status().isConflict());jdbc.execute("DROP TRIGGER it10_fail_item");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_order",Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='LOCKED'",Integer.class,zoneId)).isZero();
    }

    @Test void concurrentOrdersCannotShareSingleCandidate()throws Exception{
        jdbc.update("UPDATE match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id SET i.inventory_status='DISABLED' WHERE i.match_zone_id=? AND s.row_seq=2",zoneId);
        int[] result=concurrentCreate4();assertThat(result[0]).isEqualTo(1);assertThat(result[1]).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_order WHERE match_zone_id=? AND order_status='PENDING_PAYMENT'",Integer.class,zoneId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT lock_order_id) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='LOCKED'",Integer.class,zoneId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='LOCKED'",Integer.class,zoneId)).isEqualTo(4);
    }

    @Test void concurrentOrdersRecomputeAndUseSecondRow()throws Exception{
        int[] result=concurrentCreate4();assertThat(result[0]).isEqualTo(2);assertThat(result[1]).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT lock_order_id) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='LOCKED'",Integer.class,zoneId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT s.row_seq) FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.match_zone_id=? AND i.inventory_status='LOCKED'",Integer.class,zoneId)).isEqualTo(2);
    }

    @Test void ownershipCancelIdempotencyAndAvailabilityRecovery()throws Exception{
        create(4,userA).andExpect(status().isOk());long order=id("SELECT MAX(order_id) FROM ticket_order WHERE user_id="+userAId);
        mvc.perform(get("/api/orders").param("orderStatus","PENDING_PAYMENT").header("Authorization",bearer(userA))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].orderId").value(order));
        mvc.perform(get("/api/orders/{id}",order).header("Authorization",bearer(userB))).andExpect(status().isForbidden());
        cancel(order,userB).andExpect(status().isForbidden());
        mvc.perform(get("/api/match-ticket-zones/{id}/availability",zoneId).header("Authorization",bearer(userA))).andExpect(jsonPath("$.data.maxContinuousCount").value(4));
        cancel(order,userA).andExpect(status().isOk()).andExpect(jsonPath("$.data.order.cancelReason").value("USER_CANCELLED"));cancel(order,userA).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='CANCELLED'",Integer.class,order)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='AVAILABLE'",Integer.class,zoneId)).isEqualTo(8);
        mvc.perform(get("/api/match-ticket-zones/{id}/availability",zoneId).header("Authorization",bearer(userA))).andExpect(jsonPath("$.data.maxContinuousCount").value(4));
        preview(4,userA).andExpect(status().isOk());
    }

    @Test void timeoutAndConcurrentUserCancelRemainConsistent()throws Exception{
        create(3,userA).andExpect(status().isOk());long order=id("SELECT MAX(order_id) FROM ticket_order WHERE user_id="+userAId);jdbc.update("UPDATE ticket_order SET expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE order_id=?",order);
        assertThat(service.closeExpiredBatch()).isGreaterThanOrEqualTo(1);assertThat(jdbc.queryForObject("SELECT cancel_reason FROM ticket_order WHERE order_id=?",String.class,order)).isEqualTo("PAYMENT_TIMEOUT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=?",Integer.class,order)).isZero();
        create(4,userA).andExpect(status().isOk());long race=id("SELECT MAX(order_id) FROM ticket_order WHERE user_id="+userAId);jdbc.update("UPDATE ticket_order SET expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE order_id=?",race);
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
        try{Future<?> a=pool.submit(()->{await(barrier);try{service.cancelOwned(userAId,race);}catch(RuntimeException ignored){}});Future<?> b=pool.submit(()->{await(barrier);service.closeExpiredOrder(race);});a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?",String.class,race)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='CANCELLED'",Integer.class,race)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=?",Integer.class,race)).isZero();
    }

    private int[] concurrentCreate4()throws Exception{ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier b=new CyclicBarrier(2);AtomicInteger ok=new AtomicInteger(),conflict=new AtomicInteger();Callable<Void> callA=()->{runCreate(b,userA,ok,conflict);return null;};Callable<Void> callB=()->{runCreate(b,userB,ok,conflict);return null;};try{Future<Void>a=pool.submit(callA),c=pool.submit(callB);a.get(15,TimeUnit.SECONDS);c.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}return new int[]{ok.get(),conflict.get()};}
    private void runCreate(CyclicBarrier b,String token,AtomicInteger ok,AtomicInteger conflict){await(b);try{int status=create(4,token).andReturn().getResponse().getStatus();if(status==200)ok.incrementAndGet();else if(status==409)conflict.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}}
    private void await(CyclicBarrier b){try{b.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
    private ResultActions create(int n,String token)throws Exception{return mvc.perform(post("/api/orders").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zoneId,"ticketCount",n))));}
    private ResultActions cancel(long id,String token)throws Exception{return mvc.perform(post("/api/orders/{id}/cancel",id).header("Authorization",bearer(token)));}
    private ResultActions preview(int n,String token)throws Exception{return mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",zoneId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":"+n+"}"));}
    private String loginByPhone(String phone)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("phone",phone,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private void cleanup(){
        jdbc.execute("DROP TRIGGER IF EXISTS it10_fail_item");
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10')))");
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10'))");
        jdbc.update("DELETE FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10')");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT10')");jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT10'");jdbc.update("DELETE FROM sys_user WHERE username='it10_user2'");
    }
}
