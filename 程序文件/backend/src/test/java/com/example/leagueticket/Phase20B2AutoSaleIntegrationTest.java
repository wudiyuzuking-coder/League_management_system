package com.example.leagueticket;

import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.service.TicketSalePolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class Phase20B2AutoSaleIntegrationTest {
    private static final String SEASON="IT20B2赛季";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired SystemTimeService systemTimeService;
    @Autowired TicketSalePolicy salePolicy;

    long seasonId,roundId,homeClubId,awayClubId,stadiumId,matchId,staticZoneA,staticZoneB;
    String eventAdmin,user;

    @BeforeEach void setup() throws Exception {
        cleanup();
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        Map<String,Object> home=jdbc.queryForMap("SELECT club_id,home_stadium_id FROM club_info WHERE home_stadium_id IS NOT NULL ORDER BY club_id LIMIT 1");
        homeClubId=((Number)home.get("club_id")).longValue();stadiumId=((Number)home.get("home_stadium_id")).longValue();
        awayClubId=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_id<>? ORDER BY club_id LIMIT 1",Long.class,homeClubId);
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES(?, '2027-06-01','2027-06-30','ACTIVE')",SEASON);
        seasonId=id("SELECT season_id FROM season_info WHERE season_name='"+SEASON+"'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT20B2轮次','2027-06-01','2027-06-30','PUBLISHED')",seasonId);
        roundId=id("SELECT round_id FROM round_info WHERE season_id="+seasonId);
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(?,?,?,?,?,'2027-06-10 19:30:00','PUBLISHED','2027-05-01 10:00:00')",seasonId,roundId,homeClubId,awayClubId,stadiumId);
        matchId=id("SELECT match_id FROM match_info WHERE season_id="+seasonId);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT20B2A','IT20B2东区',201,'ACTIVE'),(?,'IT20B2B','IT20B2西区',202,'ACTIVE')",stadiumId,stadiumId);
        staticZoneA=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT20B2A'");
        staticZoneB=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT20B2B'");
        for(long zone:new long[]{staticZoneA,staticZoneB})for(int seat=1;seat<=4;seat++)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'1排',1,?,?,0,'ACTIVE')",stadiumId,zone,seat+"座",seat);
        eventAdmin=login("13800000005");user=login("13800000001");
    }

    @AfterEach void teardown(){jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");cleanup();}

    @Test void automaticFormulaIsSevenCalendarDaysEarlierAtTwentyHundredAndClientValueIsIgnored() throws Exception {
        assertThat(salePolicy.calculateSaleStartTime(LocalDateTime.parse("2027-06-10T19:30:00"))).isEqualTo(LocalDateTime.parse("2027-06-03T20:00:00"));
        long zone=createZone(staticZoneA,"2027-06-10T19:00:00","2020-01-01T00:00:00");
        assertThat(jdbc.queryForObject("SELECT sale_start_time FROM match_ticket_zone WHERE match_zone_id=?",LocalDateTime.class,zone)).isEqualTo(LocalDateTime.parse("2027-06-03T20:00:00"));
    }

    @Test void preEnableThen1959RejectsAnd2000AutomaticallyAllowsQueryPreviewAndOrder() throws Exception {
        long zone=createReadyZone(staticZoneA,"2027-06-10T19:00:00");
        setTime("2027-06-01T12:00:00");transition(zone,"ON_SALE").andExpect(status().isOk());
        setTime("2027-06-03T19:59:59");
        detail(zone).andExpect(status().isOk()).andExpect(jsonPath("$.data.saleStartTime").value("2027-06-03T20:00:00")).andExpect(jsonPath("$.data.saleAvailable").value(false)).andExpect(jsonPath("$.data.saleState").value("NOT_STARTED"));
        preview(zone,2).andExpect(status().isConflict());order(zone,2).andExpect(status().isConflict());
        setTime("2027-06-03T20:00:00");
        detail(zone).andExpect(status().isOk()).andExpect(jsonPath("$.data.saleAvailable").value(true)).andExpect(jsonPath("$.data.saleState").value("AVAILABLE"));
        preview(zone,2).andExpect(status().isOk()).andExpect(jsonPath("$.data.ticketCount").value(2));
        order(zone,2).andExpect(status().isOk()).andExpect(jsonPath("$.data.order.orderStatus").value("PENDING_PAYMENT"));
    }

    @Test void draftPausedClosedAndZeroInventoryNeverBecomePurchasable() throws Exception {
        setTime("2027-06-03T20:00:00");
        long draft=createReadyZone(staticZoneA,"2027-06-10T19:00:00");
        jdbc.update("UPDATE match_info SET match_status='DRAFT' WHERE match_id=?",matchId);
        preview(draft,1).andExpect(status().isConflict());order(draft,1).andExpect(status().isConflict());
        jdbc.update("UPDATE match_info SET match_status='PUBLISHED' WHERE match_id=?",matchId);transition(draft,"ON_SALE").andExpect(status().isOk());transition(draft,"PAUSED").andExpect(status().isOk());
        detail(draft).andExpect(jsonPath("$.data.saleState").value("PAUSED")).andExpect(jsonPath("$.data.saleAvailable").value(false));preview(draft,1).andExpect(status().isConflict());
        transition(draft,"CLOSED").andExpect(status().isOk());detail(draft).andExpect(jsonPath("$.data.saleState").value("CLOSED"));
        long empty=createZone(staticZoneB,"2027-06-10T19:00:00",null);jdbc.update("UPDATE match_ticket_zone SET zone_status='ON_SALE' WHERE match_zone_id=?",empty);
        detail(empty).andExpect(jsonPath("$.data.saleState").value("SOLD_OUT")).andExpect(jsonPath("$.data.saleAvailable").value(false));preview(empty,1).andExpect(status().isConflict());
    }

    @Test void saleEndBoundaryStillUsesExclusiveCurrentRule() throws Exception {
        long zone=createReadyZone(staticZoneA,"2027-06-04T10:00:00");setTime("2027-06-01T12:00:00");transition(zone,"ON_SALE").andExpect(status().isOk());
        setTime("2027-06-04T09:59:59");detail(zone).andExpect(jsonPath("$.data.saleAvailable").value(true));
        setTime("2027-06-04T10:00:00");detail(zone).andExpect(jsonPath("$.data.saleAvailable").value(false)).andExpect(jsonPath("$.data.saleState").value("ENDED"));preview(zone,1).andExpect(status().isConflict());order(zone,1).andExpect(status().isConflict());
    }

    @Test void rescheduleSynchronizesEveryZoneAndConflictRollsBackEverything() throws Exception {
        jdbc.update("UPDATE match_info SET match_time='2027-06-20 19:30:00' WHERE match_id=?",matchId);
        long a=createZone(staticZoneA,"2027-06-20T19:00:00",null),b=createZone(staticZoneB,"2027-06-20T18:00:00",null);
        updateMatch("2027-06-25T19:30:00").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_ticket_zone WHERE match_id=? AND sale_start_time='2027-06-18 20:00:00'",Integer.class,matchId)).isEqualTo(2);
        LocalDateTime oldMatch=jdbc.queryForObject("SELECT match_time FROM match_info WHERE match_id=?",LocalDateTime.class,matchId);
        Map<Long,LocalDateTime> starts=Map.of(a,jdbc.queryForObject("SELECT sale_start_time FROM match_ticket_zone WHERE match_zone_id=?",LocalDateTime.class,a),b,jdbc.queryForObject("SELECT sale_start_time FROM match_ticket_zone WHERE match_zone_id=?",LocalDateTime.class,b));
        updateMatch("2027-06-28T19:30:00").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT match_time FROM match_info WHERE match_id=?",LocalDateTime.class,matchId)).isEqualTo(oldMatch);
        starts.forEach((id,start)->assertThat(jdbc.queryForObject("SELECT sale_start_time FROM match_ticket_zone WHERE match_zone_id=?",LocalDateTime.class,id)).isEqualTo(start));
    }

    @Test void paidOrderRemainsIntactWhenDelayMovesAutomaticStartIntoFuture() throws Exception {
        jdbc.update("UPDATE match_info SET match_time='2027-06-20 19:30:00' WHERE match_id=?",matchId);
        long zone=createReadyZone(staticZoneA,"2027-06-20T19:00:00");setTime("2027-06-13T20:00:00");transition(zone,"ON_SALE").andExpect(status().isOk());
        long orderId=json.readTree(order(zone,1).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("order").path("orderId").asLong();
        mvc.perform(post("/api/orders/{id}/pay",orderId).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"payMethod\":\"SIMULATED\",\"simulateResult\":\"SUCCESS\"}")).andExpect(status().isOk());
        updateMatch("2027-06-25T19:30:00").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?",String.class,orderId)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT item_status FROM order_item WHERE order_id=?",String.class,orderId)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT i.inventory_status FROM match_seat_inventory i JOIN order_item oi ON oi.inventory_id=i.inventory_id WHERE oi.order_id=?",String.class,orderId)).isEqualTo("SOLD");
        assertThat(jdbc.queryForObject("SELECT ticket_status FROM e_ticket WHERE order_id=?",String.class,orderId)).isEqualTo("UNUSED");
        detail(zone).andExpect(jsonPath("$.data.saleAvailable").value(false)).andExpect(jsonPath("$.data.saleState").value("NOT_STARTED"));
    }

    @Test void advancingMatchMakesPreviouslyFutureAutomaticStartImmediatelyAvailable() throws Exception {
        jdbc.update("UPDATE match_info SET match_time='2027-06-12 19:30:00' WHERE match_id=?",matchId);
        long zone=createReadyZone(staticZoneA,"2027-06-09T19:00:00");setTime("2027-06-04T12:00:00");transition(zone,"ON_SALE").andExpect(status().isOk());
        detail(zone).andExpect(jsonPath("$.data.saleAvailable").value(false));
        updateMatch("2027-06-10T19:30:00").andExpect(status().isOk());
        detail(zone).andExpect(jsonPath("$.data.saleStartTime").value("2027-06-03T20:00:00")).andExpect(jsonPath("$.data.saleAvailable").value(true));
    }

    private long createReadyZone(long staticZone,String end) throws Exception {long id=createZone(staticZone,end,null);mvc.perform(post("/api/admin/match-ticket-zones/{id}/inventory/generate",id).header("Authorization",bearer(eventAdmin))).andExpect(status().isOk());return id;}
    private long createZone(long staticZone,String end,String legacyStart) throws Exception {Map<String,Object> body=new LinkedHashMap<>();body.put("stadiumZoneId",staticZone);body.put("price",100);body.put("saleEndTime",end);if(legacyStart!=null)body.put("saleStartTime",legacyStart);String response=mvc.perform(post("/api/admin/matches/{id}/ticket-zones",matchId).header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(response).path("data").path("matchZoneId").asLong();}
    private ResultActions transition(long zone,String status) throws Exception {return mvc.perform(put("/api/admin/match-ticket-zones/{id}/status",zone).header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("zoneStatus",status))));}
    private ResultActions detail(long zone) throws Exception {return mvc.perform(get("/api/match-ticket-zones/{id}",zone).header("Authorization",bearer(user)));}
    private ResultActions preview(long zone,int count) throws Exception {return mvc.perform(post("/api/match-ticket-zones/{id}/seat-allocation/preview",zone).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{\"ticketCount\":"+count+"}"));}
    private ResultActions order(long zone,int count) throws Exception {return mvc.perform(post("/api/orders").header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchZoneId",zone,"ticketCount",count))));}
    private ResultActions updateMatch(String time) throws Exception {Map<String,Object> body=new LinkedHashMap<>();body.put("seasonId",seasonId);body.put("roundId",roundId);body.put("homeClubId",homeClubId);body.put("awayClubId",awayClubId);body.put("stadiumId",stadiumId);body.put("matchTime",time);return mvc.perform(put("/api/admin/matches/{id}",matchId).header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));}
    private void setTime(String target){long millis=Duration.between(systemTimeService.realNow(),LocalDateTime.parse(target)).toMillis();long seconds=Math.floorDiv(millis+999,1000);jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'",Long.toString(seconds));}
    private String login(String phone) throws Exception {String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(body).path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private void cleanup(){
        jdbc.update("DELETE FROM e_ticket WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)))",SEASON);
        jdbc.update("DELETE FROM payment_record WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)))",SEASON);
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)))",SEASON);
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?))",SEASON);
        jdbc.update("DELETE FROM ticket_order WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?))",SEASON);
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?))",SEASON);
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)",SEASON);
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)",SEASON);
        jdbc.update("DELETE FROM season_info WHERE season_name=?",SEASON);
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code IN ('IT20B2A','IT20B2B'))");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code IN ('IT20B2A','IT20B2B')");
    }
}
