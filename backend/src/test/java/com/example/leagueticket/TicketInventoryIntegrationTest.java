package com.example.leagueticket;

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
import org.springframework.test.web.servlet.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class TicketInventoryIntegrationTest {
    @Autowired MockMvc mockMvc;@Autowired ObjectMapper objectMapper;@Autowired JdbcTemplate jdbc;
    String admin,systemAdmin,user,club;long matchA,matchB,activeZone,disabledZone,otherZone;

    @BeforeEach void reset()throws Exception{
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季'))");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季')");
        jdbc.update("DELETE FROM season_info WHERE season_name='IT8赛季'");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code LIKE 'IT8%')");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code LIKE 'IT8%'");
        var stadiums=jdbc.queryForList("SELECT home_stadium_id stadium_id,MIN(club_id) club_a,MAX(club_id) club_b FROM club_info WHERE home_stadium_id IS NOT NULL GROUP BY home_stadium_id HAVING COUNT(*)>=2 ORDER BY home_stadium_id LIMIT 1");
        long stadium=((Number)stadiums.get(0).get("stadium_id")).longValue(),clubA=((Number)stadiums.get(0).get("club_a")).longValue(),clubB=((Number)stadiums.get(0).get("club_b")).longValue();
        long otherStadium=jdbc.queryForObject("SELECT stadium_id FROM stadium_info WHERE stadium_id<>? ORDER BY stadium_id LIMIT 1",Long.class,stadium);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT8ACTIVE','IT8测试区',90,'ACTIVE'),(?,'IT8DISABLED','IT8停用区',91,'DISABLED'),(?,'IT8OTHER','IT8其他场区',92,'ACTIVE')",stadium,stadium,otherStadium);
        activeZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8ACTIVE'");disabledZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8DISABLED'");otherZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8OTHER'");
        int[][] numbers={{1,1},{1,2},{1,3},{1,5},{1,6},{2,1},{2,2},{2,3},{2,4}};for(int[] n:numbers)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,? ,?,?,?, ?,0,'ACTIVE')",stadium,activeZone,n[0]+"排",n[0],n[1]+"座",n[1]);
        jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'2排',2,'5座',5,0,'DISABLED')",stadium,activeZone);
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT8赛季',CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),'ACTIVE')");long season=id("SELECT season_id FROM season_info WHERE season_name='IT8赛季'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT8轮次',CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 7 DAY),'PUBLISHED')",season);long round=id("SELECT round_id FROM round_info WHERE season_id="+season);
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 2 DAY),'PUBLISHED',NOW()),(?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 3 DAY),'PUBLISHED',NOW())",season,round,clubA,clubB,stadium,season,round,clubB,clubA,stadium);
        var matches=jdbc.queryForList("SELECT match_id FROM match_info WHERE season_id=? ORDER BY match_id",season);matchA=((Number)matches.get(0).get("match_id")).longValue();matchB=((Number)matches.get(1).get("match_id")).longValue();
        admin=loginByPhone("13800000005");systemAdmin=loginByPhone("13800000002");user=loginByPhone("13800000001");club=loginByPhone("13800000003");
    }

    @Test void ticketZoneValidationCreatorAndPermissions()throws Exception{
        long zone=createZone(matchA,activeZone,admin);
        long adminId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='demo_event_admin'",Long.class);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT created_by FROM match_ticket_zone WHERE match_zone_id=?",Long.class,zone)).isEqualTo(adminId);
        createZoneRequest(matchA,activeZone,user,zoneBody(activeZone,100)).andExpect(status().isForbidden());
        createZoneRequest(matchA,activeZone,systemAdmin,zoneBody(activeZone,100)).andExpect(status().isForbidden());
        createZoneRequest(matchA,activeZone,admin,zoneBody(activeZone,100)).andExpect(status().isConflict());
        createZoneRequest(matchA,disabledZone,admin,zoneBody(disabledZone,100)).andExpect(status().isBadRequest());
        createZoneRequest(matchA,otherZone,admin,zoneBody(otherZone,100)).andExpect(status().isBadRequest());
        createZoneRequest(matchA,activeZone,admin,zoneBody(activeZone,-1)).andExpect(status().isBadRequest());
        Map<String,Object> bad=zoneBody(activeZone,10);bad.put("saleEndTime",LocalDateTime.now().minusHours(2));createZoneRequest(matchB,activeZone,admin,bad).andExpect(status().isBadRequest());
        Map<String,Object> late=zoneBody(activeZone,10);late.put("saleEndTime",LocalDateTime.now().plusDays(3));createZoneRequest(matchB,activeZone,admin,late).andExpect(status().isBadRequest());
    }

    @Test void inventoryGenerationCopiesActiveSeatsAndIsIndependent()throws Exception{
        long zoneA=createZone(matchA,activeZone,admin),zoneB=createZone(matchB,activeZone,admin);
        generate(zoneA,admin).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(9));
        generate(zoneA,admin).andExpect(status().isConflict());
        generate(zoneB,admin).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(9));
        long first=id("SELECT MIN(inventory_id) FROM match_seat_inventory WHERE match_zone_id="+zoneA);
        inventoryStatus(first,"DISABLED",admin).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='DISABLED'",Long.class,zoneB)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id IN (?,?)",Long.class,zoneA,zoneB)).isEqualTo(18);
        inventoryStatus(first,"SOLD",admin).andExpect(status().isBadRequest());
    }

    @Test void statusMachineAvailabilityContinuityAndReadPermissions()throws Exception{
        long empty=createZone(matchA,activeZone,admin);transition(empty,"ON_SALE",admin).andExpect(status().isBadRequest());
        generate(empty,admin).andExpect(status().isOk());transition(empty,"ON_SALE",admin).andExpect(status().isOk());transition(empty,"PAUSED",admin).andExpect(status().isOk());transition(empty,"ON_SALE",admin).andExpect(status().isOk());
        mockMvc.perform(get("/api/match-ticket-zones/{id}/availability",empty).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(9)).andExpect(jsonPath("$.data.availableSeatCount").value(9)).andExpect(jsonPath("$.data.maxContinuousCount").value(4));
        long row2Seat3=jdbc.queryForObject("SELECT i.inventory_id FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.match_zone_id=? AND s.row_seq=2 AND s.seat_seq=3",Long.class,empty);
        inventoryStatus(row2Seat3,"DISABLED",admin).andExpect(status().isOk());
        mockMvc.perform(get("/api/match-ticket-zones/{id}/availability",empty).header("Authorization",bearer(club))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(9)).andExpect(jsonPath("$.data.availableSeatCount").value(8)).andExpect(jsonPath("$.data.disabledSeatCount").value(1)).andExpect(jsonPath("$.data.maxContinuousCount").value(3));
        mockMvc.perform(get("/api/matches/{id}/ticket-zones",matchA).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].saleAvailable").value(true));
        inventoryStatus(row2Seat3,"AVAILABLE",admin).andExpect(status().isOk());
        transition(empty,"CLOSED",admin).andExpect(status().isOk());transition(empty,"ON_SALE",admin).andExpect(status().isBadRequest());
        createZoneRequest(matchB,activeZone,user,zoneBody(activeZone,50)).andExpect(status().isForbidden());
    }

    private long createZone(long match,long stadiumZone,String token)throws Exception{String body=createZoneRequest(match,stadiumZone,token,zoneBody(stadiumZone,100)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("matchZoneId").asLong();}
    private ResultActions createZoneRequest(long match,long stadiumZone,String token,Map<String,Object> body)throws Exception{return mockMvc.perform(post("/api/admin/matches/{id}/ticket-zones",match).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(body)));}
    private ResultActions generate(long zone,String token)throws Exception{return mockMvc.perform(post("/api/admin/match-ticket-zones/{id}/inventory/generate",zone).header("Authorization",bearer(token)));}
    private ResultActions transition(long zone,String value,String token)throws Exception{return mockMvc.perform(put("/api/admin/match-ticket-zones/{id}/status",zone).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("zoneStatus",value))));}
    private ResultActions inventoryStatus(long id,String value,String token)throws Exception{return mockMvc.perform(put("/api/admin/match-seat-inventory/{id}/status",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("inventoryStatus",value))));}
    private Map<String,Object> zoneBody(long zone,double price){Map<String,Object> body=new LinkedHashMap<>();body.put("stadiumZoneId",zone);body.put("price",price);body.put("saleStartTime",LocalDateTime.now().minusHours(1));body.put("saleEndTime",LocalDateTime.now().plusHours(24));return body;}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private String loginByPhone(String phone)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
