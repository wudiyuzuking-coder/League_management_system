package com.example.leagueticket;

import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class SeatAllocationIntegrationTest {
    @Autowired MockMvc mockMvc;@Autowired ObjectMapper objectMapper;@Autowired JdbcTemplate jdbc;@Autowired SeatAllocateService service;
    long matchZoneId;String admin,systemAdmin,user,club;

    @BeforeEach void reset()throws Exception{
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT9'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT9')");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT9')");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT9'");
        long matchId=jdbc.queryForObject("SELECT match_id FROM match_info WHERE match_status='PUBLISHED' ORDER BY match_id LIMIT 1",Long.class);
        long stadiumId=jdbc.queryForObject("SELECT stadium_id FROM match_info WHERE match_id=?",Long.class,matchId);
        long adminId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='demo_event_admin'",Long.class);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT9','IT9连坐测试区',99,'ACTIVE')",stadiumId);
        long staticZone=jdbc.queryForObject("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT9'",Long.class);
        for(int seat=1;seat<=4;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'1排',1,?, ?,0,'ACTIVE')",stadiumId,staticZone,seat+"座",seat);
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT9连坐测试区',99,'ON_SALE',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 1 DAY))",matchId,staticZone,adminId);
        matchZoneId=jdbc.queryForObject("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id=?",Long.class,staticZone);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=?",matchId,matchZoneId,staticZone);
        admin=loginByPhone("13800000005");systemAdmin=loginByPhone("13800000002");user=loginByPhone("13800000001");club=loginByPhone("13800000003");
    }

    @Test void previewRolesDebugPermissionValidationAndNoSideEffects()throws Exception{
        List<Map<String,Object>> before=jdbc.queryForList("SELECT inventory_id,inventory_status,version FROM match_seat_inventory WHERE match_zone_id=? ORDER BY inventory_id",matchZoneId);
        for(String token:List.of(user,club,admin))preview(2,token).andExpect(status().isOk()).andExpect(jsonPath("$.data.seatNos[0]").value(2)).andExpect(jsonPath("$.data.seatNos[1]").value(3));
        preview(0,user).andExpect(status().isBadRequest());preview(5,user).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/match-ticket-zones/{id}/seat-allocation/debug",matchZoneId).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":2}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/match-ticket-zones/{id}/seat-allocation/debug",matchZoneId).header("Authorization",bearer(systemAdmin)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":2}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/match-ticket-zones/{id}/seat-allocation/debug",matchZoneId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":2}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.candidates.length()").value(3)).andExpect(jsonPath("$.data.best.seatNos[0]").value(2));
        List<Map<String,Object>> after=jdbc.queryForList("SELECT inventory_id,inventory_status,version FROM match_seat_inventory WHERE match_zone_id=? ORDER BY inventory_id",matchZoneId);
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }

    @Test void noSolutionReturnsMaxContinuousAndMatchesStage8Availability()throws Exception{
        long second=jdbc.queryForObject("SELECT i.inventory_id FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.match_zone_id=? AND s.seat_seq=2",Long.class,matchZoneId);
        jdbc.update("UPDATE match_seat_inventory SET inventory_status='DISABLED' WHERE inventory_id=?",second);
        String body=preview(4,user).andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(body).path("message").asText()).contains("maxContinuousCount=2");
        mockMvc.perform(get("/api/match-ticket-zones/{id}/availability",matchZoneId).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data.maxContinuousCount").value(2));
        mockMvc.perform(post("/api/admin/match-ticket-zones/{id}/seat-allocation/debug",matchZoneId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":4}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.maxContinuousCount").value(2)).andExpect(jsonPath("$.data.best").doesNotExist());
    }

    @Test void concurrentConditionalClaimsAllowAtMostOneWinnerAndNoPartialUpdate()throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier start=new CyclicBarrier(2);AtomicInteger success=new AtomicInteger(),conflict=new AtomicInteger();
        Callable<Void> task=()->{start.await(5,TimeUnit.SECONDS);try{service.selectAndClaimAvailable(matchZoneId,4);success.incrementAndGet();}catch(RuntimeException expected){conflict.incrementAndGet();}return null;};
        try{Future<Void> one=pool.submit(task),two=pool.submit(task);one.get(10,TimeUnit.SECONDS);two.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        org.assertj.core.api.Assertions.assertThat(success.get()).isEqualTo(1);org.assertj.core.api.Assertions.assertThat(conflict.get()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='DISABLED'",Long.class,matchZoneId)).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND version=1",Long.class,matchZoneId)).isEqualTo(4);
    }

    private org.springframework.test.web.servlet.ResultActions preview(int count,String token)throws Exception{return mockMvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",matchZoneId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("ticketCount",count))));}
    private String loginByPhone(String phone)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("phone",phone,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
