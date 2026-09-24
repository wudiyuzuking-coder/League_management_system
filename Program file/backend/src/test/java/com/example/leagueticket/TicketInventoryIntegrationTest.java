package com.example.leagueticket;

import com.example.leagueticket.dto.MatchTicketZoneRequest;
import com.example.leagueticket.service.MatchSeatInventoryService;
import com.example.leagueticket.service.MatchTicketZoneService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class TicketInventoryIntegrationTest {
    @Autowired MockMvc mockMvc;@Autowired ObjectMapper objectMapper;@Autowired JdbcTemplate jdbc;
    @Autowired MatchTicketZoneService zoneService;@Autowired MatchSeatInventoryService inventoryService;
    String admin,systemAdmin,user,club;long matchA,matchB,activeZone,disabledZone,otherZone;

    @BeforeEach void reset()throws Exception{
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季'))");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT8赛季')");
        jdbc.update("DELETE FROM season_info WHERE season_name='IT8赛季'");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code LIKE 'IT8%')");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code LIKE 'IT8%'");
        long stadium=jdbc.queryForObject("SELECT stadium_id FROM stadium_info ORDER BY stadium_id LIMIT 1",Long.class);
        var clubs=jdbc.queryForList("SELECT club_id FROM club_info ORDER BY club_id LIMIT 2");
        long clubA=((Number)clubs.get(0).get("club_id")).longValue(),clubB=((Number)clubs.get(1).get("club_id")).longValue();
        long otherStadium=jdbc.queryForObject("SELECT stadium_id FROM stadium_info WHERE stadium_id<>? ORDER BY stadium_id LIMIT 1",Long.class,stadium);
        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT8ACTIVE','IT8测试区',90,'ACTIVE'),(?,'IT8DISABLED','IT8停用区',91,'DISABLED'),(?,'IT8OTHER','IT8其他场区',92,'ACTIVE')",stadium,stadium,otherStadium);
        activeZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8ACTIVE'");disabledZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8DISABLED'");otherZone=id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT8OTHER'");
        int[][] numbers={{1,1},{1,2},{1,3},{1,5},{1,6},{2,1},{2,2},{2,3},{2,4}};for(int[] n:numbers)jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,? ,?,?,?, ?,0,'ACTIVE')",stadium,activeZone,n[0]+"排",n[0],n[1]+"座",n[1]);
        jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'2排',2,'5座',5,0,'DISABLED')",stadium,activeZone);
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT8赛季',CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),'IN_PROGRESS')");long season=id("SELECT season_id FROM season_info WHERE season_name='IT8赛季'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT8轮次',CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 7 DAY),'PUBLISHED')",season);long round=id("SELECT round_id FROM round_info WHERE season_id="+season);
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 2 DAY),'PUBLISHED',NOW()),(?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 3 DAY),'PUBLISHED',NOW())",season,round,clubA,clubB,stadium,season,round,clubB,clubA,stadium);
        var matches=jdbc.queryForList("SELECT match_id FROM match_info WHERE season_id=? ORDER BY match_id",season);matchA=((Number)matches.get(0).get("match_id")).longValue();matchB=((Number)matches.get(1).get("match_id")).longValue();
        admin=loginByPhone("13800000005");systemAdmin=loginByPhone("13800000002");user=loginByPhone("13800000001");club=loginByPhone("13800000003");
    }

    @Test void ticketZoneValidationCreatorAndPermissions()throws Exception{
        long adminId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='demo_event_admin'",Long.class);
        long zone=createZone(matchA,activeZone,adminId);
        assertThat(jdbc.queryForObject("SELECT created_by FROM match_ticket_zone WHERE match_zone_id=?",Long.class,zone)).isEqualTo(adminId);
        mockMvc.perform(post("/api/admin/matches/{id}/ticket-zones",matchA).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/admin/matches/{id}/ticket-zones",matchA).header("Authorization",bearer(user)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertThatThrownBy(()->zoneService.create(matchA,adminId,request(activeZone,100))).hasMessageContaining("already configured");
        assertThatThrownBy(()->zoneService.create(matchA,adminId,request(disabledZone,100))).hasMessageContaining("disabled");
        assertThatThrownBy(()->zoneService.create(matchA,adminId,request(otherZone,100))).hasMessageContaining("does not belong");
        createZone(matchB,activeZone,adminId);
        LocalDateTime matchTime=jdbc.queryForObject("SELECT match_time FROM match_info WHERE match_id=?",LocalDateTime.class,matchB);
        Map<String,Object> officialWindow=jdbc.queryForMap("SELECT sale_start_time,sale_end_time FROM match_ticket_zone WHERE match_id=? AND stadium_zone_id=?",matchB,activeZone);
        org.assertj.core.api.Assertions.assertThat((LocalDateTime)officialWindow.get("sale_start_time")).isEqualTo(matchTime.toLocalDate().minusDays(14).atTime(20,0));
        org.assertj.core.api.Assertions.assertThat((LocalDateTime)officialWindow.get("sale_end_time")).isEqualTo(matchTime.minusHours(1));
    }

    @Test void inventoryGenerationCopiesActiveSeatsAndIsIndependent()throws Exception{
        long creator=id("SELECT user_id FROM sys_user WHERE username='demo_event_admin'");
        long zoneA=createZone(matchA,activeZone,creator),zoneB=createZone(matchB,activeZone,creator);
        assertThat(inventoryService.generate(zoneA)).isEqualTo(9);
        assertThatThrownBy(()->inventoryService.generate(zoneA)).hasMessageContaining("already exists");
        assertThat(inventoryService.generate(zoneB)).isEqualTo(9);
        long first=id("SELECT MIN(inventory_id) FROM match_seat_inventory WHERE match_zone_id="+zoneA);
        inventoryService.updateStatus(first,"DISABLED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id=? AND inventory_status='DISABLED'",Long.class,zoneB)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_seat_inventory WHERE match_zone_id IN (?,?)",Long.class,zoneA,zoneB)).isEqualTo(18);
        assertThatThrownBy(()->inventoryService.updateStatus(first,"SOLD")).hasMessageContaining("AVAILABLE or DISABLED");
    }

    @Test void statusMachineAvailabilityContinuityAndReadPermissions()throws Exception{
        long creator=id("SELECT user_id FROM sys_user WHERE username='demo_event_admin'");
        long empty=createZone(matchA,activeZone,creator);
        assertThatThrownBy(()->zoneService.updateStatus(empty,"ON_SALE")).hasMessageContaining("inventory");
        inventoryService.generate(empty);zoneService.updateStatus(empty,"ON_SALE");zoneService.updateStatus(empty,"PAUSED");zoneService.updateStatus(empty,"ON_SALE");
        mockMvc.perform(get("/api/match-ticket-zones/{id}/availability",empty).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(9)).andExpect(jsonPath("$.data.availableSeatCount").value(9)).andExpect(jsonPath("$.data.maxContinuousCount").value(4));
        long row2Seat3=jdbc.queryForObject("SELECT i.inventory_id FROM match_seat_inventory i JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.match_zone_id=? AND s.row_seq=2 AND s.seat_seq=3",Long.class,empty);
        inventoryService.updateStatus(row2Seat3,"DISABLED");
        mockMvc.perform(get("/api/match-ticket-zones/{id}/availability",empty).header("Authorization",bearer(club))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(9)).andExpect(jsonPath("$.data.availableSeatCount").value(8)).andExpect(jsonPath("$.data.disabledSeatCount").value(1)).andExpect(jsonPath("$.data.maxContinuousCount").value(3));
        mockMvc.perform(get("/api/matches/{id}/ticket-zones",matchA).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].saleAvailable").value(true));
        inventoryService.updateStatus(row2Seat3,"AVAILABLE");
        zoneService.updateStatus(empty,"CLOSED");
        assertThatThrownBy(()->zoneService.updateStatus(empty,"ON_SALE")).hasMessageContaining("transition");
    }

    private long createZone(long match,long stadiumZone,long creator){return zoneService.create(match,creator,request(stadiumZone,100)).getMatchZoneId();}
    private MatchTicketZoneRequest request(long zone,double price){return new MatchTicketZoneRequest(zone,BigDecimal.valueOf(price));}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private String loginByPhone(String phone)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(TestLoginPayload.forPhone(phone,"123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
