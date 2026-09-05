package com.example.leagueticket;

import com.example.leagueticket.service.SystemTimeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class UserClubDetailIntegrationTest {
    private static final String SEASON_NAME = "IT18A公开详情赛季";
    private static final String CLUB_NAME = "IT18A公开俱乐部";
    private static final String ZONE_CODE = "IT18APUBLIC";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemTimeService timeService;

    private String userToken;
    private String clubToken;
    private String eventAdminToken;
    private String adminToken;
    private long clubId;
    private long otherClubId;
    private long finishedMatchId;
    private long nextMatchId;
    private long laterMatchId;
    private long draftMatchId;
    private long matchZoneId;
    private LocalDateTime baseTime;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        jdbc.update("INSERT INTO sys_config(config_key,config_value,value_type,description,config_status) VALUES('SYSTEM_TIME_OFFSET_SECONDS','0','INTEGER','test','ENABLED') ON DUPLICATE KEY UPDATE config_value='0',config_status='ENABLED'");
        String hash = passwordEncoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE phone IN ('13800000001','13800000002','13800000003','13800000005')", hash);
        userToken = login("13800000001");
        clubToken = login("13800000003");
        eventAdminToken = login("13800000005");
        adminToken = login("13800000002");

        baseTime = timeService.realNow().plusDays(200).truncatedTo(ChronoUnit.SECONDS);
        setSystemTime(baseTime);
        long stadiumId = id("SELECT MIN(stadium_id) FROM stadium_info WHERE stadium_status='ACTIVE'");
        otherClubId = id("SELECT MIN(club_id) FROM club_info WHERE club_status='ACTIVE'");
        jdbc.update("INSERT INTO club_info(club_name,short_name,home_city,home_stadium_id,description,club_status) VALUES(?,?,?,?,?,'ACTIVE')",
                CLUB_NAME, "IT18A", "测试城", stadiumId, "仅包含可公开信息的测试俱乐部");
        clubId = id("SELECT club_id FROM club_info WHERE club_name='" + CLUB_NAME + "'");

        jdbc.update("INSERT INTO player_info(club_id,player_name,shirt_no,position,nationality,birth_date,player_status) VALUES" +
                        "(?, '号码靠前球员', 8, 'MIDFIELDER', '中国', ?, 'ACTIVE')," +
                        "(?, '无号码球员', NULL, 'FORWARD', '中国', ?, 'ACTIVE')," +
                        "(?, '停用球员', 1, 'GOALKEEPER', '中国', ?, 'INACTIVE')",
                clubId, baseTime.toLocalDate().minusYears(20).minusDays(1),
                clubId, baseTime.toLocalDate().minusYears(22),
                clubId, baseTime.toLocalDate().minusYears(30));
        jdbc.update("INSERT INTO coach_info(club_id,coach_name,title,nationality,coach_status) VALUES" +
                        "(?, '公开主教练', '主教练', '中国', 'ACTIVE'),(?, '停用教练', '助理教练', '中国', 'INACTIVE')",
                clubId, clubId);

        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status,description) VALUES(?,?,?,'ACTIVE','IT18A')",
                SEASON_NAME, baseTime.toLocalDate().minusDays(30), baseTime.toLocalDate().plusDays(60));
        long seasonId = id("SELECT season_id FROM season_info WHERE season_name='" + SEASON_NAME + "'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT18A轮次',?,?,'PUBLISHED')",
                seasonId, baseTime.toLocalDate().minusDays(10), baseTime.toLocalDate().plusDays(20));
        long roundId = id("SELECT round_id FROM round_info WHERE season_id=" + seasonId);
        jdbc.update("INSERT INTO club_season_record(season_id,club_id,played,wins,draws,losses,goals_for,goals_against,points) VALUES" +
                "(?,?,4,3,1,0,8,2,10),(?,?,4,2,1,1,6,4,7)", seasonId, clubId, seasonId, otherClubId);

        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,home_score,away_score,match_status,published_at) VALUES" +
                        "(?,?,?,?,?,?,2,1,'FINISHED',?),(?,?,?,?,?,?,NULL,NULL,'PUBLISHED',?),(?,?,?,?,?,?,NULL,NULL,'PUBLISHED',?),(?,?,?,?,?,?,NULL,NULL,'DRAFT',NULL)",
                seasonId, roundId, clubId, otherClubId, stadiumId, baseTime.minusDays(3), baseTime.minusDays(10),
                seasonId, roundId, clubId, otherClubId, stadiumId, baseTime.plusDays(5), baseTime.minusDays(2),
                seasonId, roundId, otherClubId, clubId, stadiumId, baseTime.plusDays(10), baseTime.minusDays(2),
                seasonId, roundId, clubId, otherClubId, stadiumId, baseTime.plusDays(2));
        var matches = jdbc.queryForList("SELECT match_id,match_status,match_time FROM match_info WHERE season_id=? ORDER BY match_time", seasonId);
        finishedMatchId = number(matches.get(0).get("match_id"));
        draftMatchId = number(matches.get(1).get("match_id"));
        nextMatchId = number(matches.get(2).get("match_id"));
        laterMatchId = number(matches.get(3).get("match_id"));

        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,?,'IT18A公开票区',180,'ACTIVE')", stadiumId, ZONE_CODE);
        long stadiumZoneId = id("SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='" + ZONE_CODE + "'");
        for (int row = 1; row <= 2; row++) for (int seat = 1; seat <= 2; seat++) {
            jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,?, ?,?, ?,0,'ACTIVE')",
                    stadiumId, stadiumZoneId, row + "排", row, seat + "座", seat);
        }
        jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'2排',2,'3座',3,0,'DISABLED')",
                stadiumId, stadiumZoneId);
        long creator = id("SELECT user_id FROM sys_user WHERE phone='13800000005'");
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT18A公开票区',100,'ON_SALE',?,?)",
                nextMatchId, stadiumZoneId, creator, baseTime.minusHours(1), baseTime.plusHours(12));
        matchZoneId = id("SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id=" + stadiumZoneId);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,'AVAILABLE' FROM stadium_seat WHERE stadium_zone_id=? AND seat_status='ACTIVE'",
                nextMatchId, matchZoneId, stadiumZoneId);
    }

    @AfterEach
    void teardown() {
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        cleanup();
    }

    @Test
    void detailReturnsPublicClubPlayersCoachesStandingAndMatches() throws Exception {
        JsonNode data = detail().path("data");
        assertThat(data.path("club").path("clubName").asText()).isEqualTo(CLUB_NAME);
        assertThat(data.path("players")).hasSize(2);
        assertThat(data.path("coaches")).hasSize(1);
        assertThat(data.path("standing").path("points").asInt()).isEqualTo(10);
        assertThat(data.path("standing").path("rank").asInt()).isEqualTo(1);
        assertThat(data.path("recentMatches").get(0).path("matchId").asLong()).isEqualTo(finishedMatchId);
        assertThat(data.path("upcomingMatches").get(0).path("matchId").asLong()).isEqualTo(nextMatchId);
        assertThat(data.path("nextMatch").path("matchId").asLong()).isEqualTo(nextMatchId);
        assertThat(data.path("daysUntilNextMatch").asLong()).isEqualTo(5);
    }

    @Test
    void responseExcludesPrivateAndTechnicalFields() throws Exception {
        String body = mvc.perform(get("/api/user/clubs/{id}", clubId).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("employeeNo", "phone", "userId", "clubStatus", "playerStatus",
                "coachStatus", "birthDate", "createdAt", "updatedAt", "enrollment", "lockOrderId");
    }

    @Test
    void activeFiltersSortAndAgeUseSystemTime() throws Exception {
        JsonNode data = detail().path("data");
        assertThat(data.path("players").get(0).path("number").asInt()).isEqualTo(8);
        assertThat(data.path("players").get(0).path("age").asInt()).isEqualTo(20);
        assertThat(data.path("players").get(1).path("name").asText()).isEqualTo("无号码球员");
        setSystemTime(baseTime.plusYears(1));
        assertThat(detail().path("data").path("players").get(0).path("age").asInt()).isEqualTo(21);
    }

    @Test
    void recentUpcomingAndDraftVisibilityFollowPublicRules() throws Exception {
        JsonNode data = detail().path("data");
        assertThat(data.path("recentMatches")).hasSize(1);
        assertThat(data.path("recentMatches").get(0).path("homeScore").asInt()).isEqualTo(2);
        assertThat(data.path("upcomingMatches")).hasSize(2);
        assertThat(data.path("upcomingMatches").toString()).doesNotContain(String.valueOf(draftMatchId), "DRAFT");
        assertThat(data.path("upcomingMatches").get(0).path("homeScore").isNull()).isTrue();
    }

    @Test
    void daysUntilNextMatchChangesWithSystemTimeAndNeverGoesNegative() throws Exception {
        assertThat(detail().path("data").path("daysUntilNextMatch").asLong()).isEqualTo(5);
        setSystemTime(baseTime.plusDays(5).minusMinutes(1));
        assertThat(detail().path("data").path("daysUntilNextMatch").asLong()).isZero();
        setSystemTime(baseTime.plusDays(6));
        JsonNode data = detail().path("data");
        assertThat(data.path("nextMatch").path("matchId").asLong()).isEqualTo(laterMatchId);
        assertThat(data.path("daysUntilNextMatch").asLong()).isEqualTo(4);
    }

    @Test
    void missingDisabledAndRolePermissionsAreEnforced() throws Exception {
        mvc.perform(get("/api/user/clubs/{id}", clubId)).andExpect(status().isUnauthorized());
        for (String token : new String[]{clubToken, eventAdminToken, adminToken}) {
            mvc.perform(get("/api/user/clubs/{id}", clubId).header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/user/clubs/{id}", Long.MAX_VALUE).header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE club_info SET club_status='DISABLED' WHERE club_id=?", clubId);
        mvc.perform(get("/api/user/clubs/{id}", clubId).header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchAndTicketZoneResponsesLinkCorrectlyWithoutInternalInventoryData() throws Exception {
        mvc.perform(get("/api/matches/{id}", nextMatchId).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeClubId").value(clubId))
                .andExpect(jsonPath("$.data.homeClubName").value(CLUB_NAME))
                .andExpect(jsonPath("$.data.awayClubId").value(otherClubId));
        JsonNode zone = response(get("/api/matches/{id}/ticket-zones", nextMatchId), userToken).path("data").get(0);
        assertThat(zone.path("matchZoneId").asLong()).isEqualTo(matchZoneId);
        assertThat(zone.path("price").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(zone.path("physicalSeatCount").asLong()).isEqualTo(5);
        assertThat(zone.path("activePhysicalSeatCount").asLong()).isEqualTo(4);
        assertThat(zone.path("rowCount").asInt()).isEqualTo(2);
        assertThat(zone.path("availableSeatCount").asLong()).isEqualTo(4);
        assertThat(zone.path("maxContinuousCount").asInt()).isEqualTo(2);
        assertThat(zone.path("saleAvailable").asBoolean()).isTrue();
        assertThat(zone.toString()).doesNotContain("createdBy", "lockedSeatCount", "lockOrderId", "version");
        mvc.perform(get("/api/matches/{id}/ticket-zones", draftMatchId).header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    private JsonNode detail() throws Exception {
        return response(get("/api/user/clubs/{id}", clubId), userToken);
    }

    private JsonNode response(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                              String token) throws Exception {
        return json.readTree(mvc.perform(request.header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private void setSystemTime(LocalDateTime target) throws Exception {
        mvc.perform(put("/api/system-time").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("targetTime", target))))
                .andExpect(status().isOk());
    }

    private String login(String phone) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone(phone, "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }

    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private long number(Object value) { return ((Number) value).longValue(); }
    private String bearer(String token) { return "Bearer " + token; }

    private void cleanup() {
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_zone_id IN (SELECT match_zone_id FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code=?))", ZONE_CODE);
        jdbc.update("DELETE FROM match_ticket_zone WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code=?)", ZONE_CODE);
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code=?)", ZONE_CODE);
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code=?", ZONE_CODE);
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)", SEASON_NAME);
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)", SEASON_NAME);
        jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name=?)", SEASON_NAME);
        jdbc.update("DELETE FROM season_info WHERE season_name=?", SEASON_NAME);
        jdbc.update("DELETE FROM player_info WHERE club_id IN (SELECT club_id FROM club_info WHERE club_name=?)", CLUB_NAME);
        jdbc.update("DELETE FROM coach_info WHERE club_id IN (SELECT club_id FROM club_info WHERE club_name=?)", CLUB_NAME);
        jdbc.update("DELETE FROM club_info WHERE club_name=?", CLUB_NAME);
        jdbc.update("DELETE FROM operation_log WHERE module_name='SYSTEM_TIME'");
    }
}
