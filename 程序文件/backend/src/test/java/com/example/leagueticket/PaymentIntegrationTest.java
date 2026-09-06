package com.example.leagueticket;

import com.example.leagueticket.dto.PaymentRequest;
import com.example.leagueticket.service.PaymentService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class PaymentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @Autowired PaymentService paymentService; @Autowired OrderService orderService;
    long zoneId,userId,otherId; String token,otherToken;

    @BeforeEach void setup()throws Exception{
        cleanup();long role=id("SELECT role_id FROM sys_role WHERE role_code='USER'");String hash=jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE username='demo_user'",String.class);
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,role_id,user_status) VALUES('it11_user2','13900001111',?,'IT11用户2',?,'ENABLED')",hash,role);
        userId=id("SELECT user_id FROM sys_user WHERE username='demo_user'");otherId=id("SELECT user_id FROM sys_user WHERE username='it11_user2'");
        long match=id("SELECT match_id FROM match_info WHERE match_status='PUBLISHED' AND match_time>DATE_ADD(NOW(),INTERVAL 1 DAY) ORDER BY match_id LIMIT 1"),stadium=id("SELECT stadium_id FROM match_info WHERE match_id="+match),admin=id("SELECT user_id FROM sys_user WHERE username='demo_admin'");
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT11','IT11支付区',111,'ACTIVE')",stadium);
        long staticZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11'");
        for(int seat=1;seat<=12;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'1排',1,?,?,0,'ACTIVE')",stadium,staticZone,seat+"座",seat);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT11支付区',66.00,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 12 HOUR))",match,staticZone,admin);
        zoneId=id("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id="+staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",match,zoneId,staticZone);
        token=loginByPhone("13800000001");otherToken=loginByPhone("13900001111");
    }
    @AfterEach void after(){jdbc.execute("DROP TRIGGER IF EXISTS it11_fail_ticket");jdbc.execute("DROP TRIGGER IF EXISTS it11_fail_inventory");cleanup();}

    @Test void successPaymentUpdatesAllStatesAndCreatesOneTicketPerItem()throws Exception{
        long last=0;
        for(int n=1;n<=4;n++){long order=create(n,token);last=order;pay(order,"SUCCESS",token).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderDetail.order.orderStatus").value("PAID")).andExpect(jsonPath("$.data.orderDetail.tickets.length()").value(n));assertThat(count("SELECT COUNT(*) FROM payment_record WHERE order_id=? AND pay_status='SUCCESS'",order)).isEqualTo(1);assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='PAID'",order)).isEqualTo(n);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='SOLD' AND i.lock_order_id IS NULL AND i.locked_at IS NULL AND i.lock_expire_time IS NULL",order)).isEqualTo(n);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=? AND ticket_status='UNUSED'",order)).isEqualTo(n);}
        mvc.perform(get("/api/tickets").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(10));
        long ticket=id("SELECT MIN(ticket_id) FROM e_ticket WHERE order_id="+last);
        mvc.perform(get("/api/tickets/{id}",ticket).header("Authorization",bearer(otherToken))).andExpect(status().isNotFound());
    }

    @Test void failedThenSuccessAndRepeatedSuccessAreSafe()throws Exception{
        long order=create(2,token);pay(order,"FAILED",token).andExpect(status().isOk()).andExpect(jsonPath("$.data.payment.payStatus").value("FAILED"));
        assertThat(orderStatus(order)).isEqualTo("PENDING_PAYMENT");assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isZero();
        pay(order,"SUCCESS",token).andExpect(status().isOk()).andExpect(jsonPath("$.data.idempotent").value(false));
        pay(order,"SUCCESS",token).andExpect(status().isOk()).andExpect(jsonPath("$.data.idempotent").value(true));
        assertThat(count("SELECT COUNT(*) FROM payment_record WHERE order_id=? AND pay_status='SUCCESS'",order)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isEqualTo(2);
    }

    @Test void ownershipExpiryAndTicketFailureRollback()throws Exception{
        long foreign=create(1,token);pay(foreign,"SUCCESS",otherToken).andExpect(status().isForbidden());assertThat(orderStatus(foreign)).isEqualTo("PENDING_PAYMENT");
        jdbc.update("UPDATE ticket_order SET expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE order_id=?",foreign);
        pay(foreign,"SUCCESS",token).andExpect(status().isConflict());assertThat(orderStatus(foreign)).isEqualTo("CANCELLED");assertThat(count("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=?",foreign)).isZero();
        long rollback=create(2,token);jdbc.execute("CREATE TRIGGER it11_fail_ticket BEFORE INSERT ON e_ticket FOR EACH ROW SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO=1062, MESSAGE_TEXT='IT11 forced ticket failure'");
        pay(rollback,"SUCCESS",token).andExpect(status().isConflict());jdbc.execute("DROP TRIGGER it11_fail_ticket");
        assertThat(orderStatus(rollback)).isEqualTo("PENDING_PAYMENT");assertThat(count("SELECT COUNT(*) FROM payment_record WHERE order_id=?",rollback)).isZero();assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='LOCKED'",rollback)).isEqualTo(2);
    }

    @Test void inventoryUpdateFailureRollsBackPaymentRecordAndOrder()throws Exception{
        long order=create(2,token);
        jdbc.execute("CREATE TRIGGER it11_fail_inventory BEFORE UPDATE ON match_seat_inventory FOR EACH ROW IF NEW.inventory_status='SOLD' THEN SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO=1062, MESSAGE_TEXT='IT11 forced inventory failure'; END IF");
        pay(order,"SUCCESS",token).andExpect(status().isConflict());jdbc.execute("DROP TRIGGER it11_fail_inventory");
        assertThat(orderStatus(order)).isEqualTo("PENDING_PAYMENT");assertThat(count("SELECT COUNT(*) FROM payment_record WHERE order_id=?",order)).isZero();assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id=? AND item_status='LOCKED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=? AND inventory_status='LOCKED'",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isZero();
    }

    @Test void concurrentDoublePaymentProducesExactlyOneSuccess()throws Exception{
        long order=create(3,token);ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
        try{Callable<Void> c=()->{barrier.await(5,TimeUnit.SECONDS);paymentService.pay(userId,order,new PaymentRequest("SIMULATED","SUCCESS"));return null;};Future<Void>a=pool.submit(c),b=pool.submit(c);a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertThat(orderStatus(order)).isEqualTo("PAID");assertThat(count("SELECT COUNT(*) FROM payment_record WHERE order_id=? AND pay_status='SUCCESS'",order)).isEqualTo(1);assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isEqualTo(3);
    }

    @Test void paymentAndTimeoutRaceEndsInOneConsistentState()throws Exception{
        long order=create(2,token);jdbc.update("UPDATE ticket_order SET expire_time=NOW(),updated_at=updated_at WHERE order_id=?",order);
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
        try{Future<?> pay=pool.submit(()->{await(barrier);try{paymentService.pay(userId,order,new PaymentRequest("SIMULATED","SUCCESS"));}catch(RuntimeException ignored){}});Future<?> timeout=pool.submit(()->{await(barrier);orderService.closeExpiredOrder(order);});pay.get(15,TimeUnit.SECONDS);timeout.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        String state=orderStatus(order);assertThat(state).isIn("PAID","CANCELLED");
        if("PAID".equals(state)){assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isEqualTo(2);assertThat(count("SELECT COUNT(*) FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=? AND i.inventory_status='SOLD'",order)).isEqualTo(2);}else{assertThat(count("SELECT COUNT(*) FROM e_ticket WHERE order_id=?",order)).isZero();assertThat(count("SELECT COUNT(*) FROM match_seat_inventory WHERE lock_order_id=?",order)).isZero();}
    }

    private long create(int n,String auth)throws Exception{String body=mvc.perform(post("/api/orders").header("Authorization",bearer(auth)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zoneId,"ticketCount",n)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("order").path("orderId").asLong();}
    private ResultActions pay(long order,String result,String auth)throws Exception{return mvc.perform(post("/api/orders/{id}/pay",order).header("Authorization",bearer(auth)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("payMethod","SIMULATED","simulateResult",result))));}
    private String loginByPhone(String phone)throws Exception{String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private void await(CyclicBarrier barrier){try{barrier.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
    private String bearer(String value){return "Bearer "+value;}private long id(String sql){return jdbc.queryForObject(sql,Long.class);}private int count(String sql,long id){return jdbc.queryForObject(sql,Integer.class,id);}private String orderStatus(long id){return jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?",String.class,id);}
    private void cleanup(){jdbc.execute("DROP TRIGGER IF EXISTS it11_fail_ticket");jdbc.execute("DROP TRIGGER IF EXISTS it11_fail_inventory");jdbc.update("DELETE FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11')))");jdbc.update("DELETE FROM payment_record WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11')))");jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11')))");jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11'))");jdbc.update("DELETE FROM ticket_order WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11'))");jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11')");jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT11')");jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT11'");jdbc.update("DELETE FROM sys_user WHERE username='it11_user2'");}
}
