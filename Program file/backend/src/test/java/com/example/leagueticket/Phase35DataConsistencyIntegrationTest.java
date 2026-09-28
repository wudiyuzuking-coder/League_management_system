package com.example.leagueticket;

import com.example.leagueticket.dto.MatchScoreRequest;
import com.example.leagueticket.service.MatchResultWorkflowService;
import com.example.leagueticket.service.StandingRecalculateService;
import com.example.leagueticket.service.SystemTimeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase35DataConsistencyIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StandingRecalculateService standings;
    @Autowired MatchResultWorkflowService results;
    @Autowired SystemTimeService time;
    @Autowired PasswordEncoder passwordEncoder;

    private List<Map<String, Object>> clubs;
    private long eventAdminId;
    private String eventToken;

    @BeforeEach
    void setup() throws Exception {
        clubs = jdbc.queryForList("SELECT club_id,home_stadium_id FROM club_info WHERE club_status='ACTIVE' AND home_stadium_id IS NOT NULL ORDER BY club_id LIMIT 4");
        assertThat(clubs).hasSize(4);
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username='demo_event_admin'", passwordEncoder.encode("123456"));
        eventAdminId = id("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE r.role_code='EVENT_ADMIN' ORDER BY u.user_id LIMIT 1");
        eventToken = login("13800000005");
    }

    @Test
    void adminDetailReturnsTheSameRealStatisticsAsTheListAndDistinguishesAnEmptySeason() throws Exception {
        long season = season("P35详情-" + System.nanoTime(), "IN_PROGRESS", 4);
        enroll(season, 0, 1, 2);
        long round = round(season);
        match(season, round, 0, 1, "PUBLISHED", null, null);
        match(season, round, 1, 2, "PUBLISHED", null, null);
        match(season, round, 2, 0, "PUBLISHED", null, null);
        jdbc.update("INSERT INTO season_schedule_batch(season_id,batch_status,trigger_type,club_count,round_count,match_count,generated_at,confirmed_at,confirmed_by) VALUES(?,'CONFIRMED','DEADLINE',3,1,3,?,?,?)",
                season, time.now().minusHours(1), time.now(), eventAdminId);

        mvc.perform(get("/api/admin/seasons/{id}", season).header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTeamCount").value(3))
                .andExpect(jsonPath("$.data.scheduleBatchStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.matchCount").value(3));
        mvc.perform(get("/api/admin/seasons").header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.seasonId == " + season + ")].submittedTeamCount").value(3))
                .andExpect(jsonPath("$.data[?(@.seasonId == " + season + ")].scheduleBatchStatus").value("CONFIRMED"));

        long empty = season("P35空赛季-" + System.nanoTime(), "DRAFT", 4);
        mvc.perform(get("/api/admin/seasons/{id}", empty).header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTeamCount").value(0))
                .andExpect(jsonPath("$.data.scheduleBatchStatus").doesNotExist())
                .andExpect(jsonPath("$.data.matchCount").value(0));
    }

    @Test
    void recalculateSelfHealsHistoricalPollutionAndIsIdempotent() {
        long season = season("P35积分-" + System.nanoTime(), "IN_PROGRESS", 4);
        enroll(season, 0, 1);
        long round = round(season);
        match(season, round, 0, 1, "FINISHED", 2, 1);
        for (int i = 0; i < 4; i++) {
            jdbc.update("INSERT INTO club_season_record(season_id,club_id,played,wins,draws,losses,goals_for,goals_against,points) VALUES(?,?,0,0,0,0,0,0,0)", season, clubId(i));
        }

        standings.recalculate(season);
        assertStandingSetAndScore(season);
        List<Map<String, Object>> first = jdbc.queryForList("SELECT club_id,played,wins,draws,losses,goals_for,goals_against,points FROM club_season_record WHERE season_id=? ORDER BY club_id", season);

        standings.recalculate(season);
        assertStandingSetAndScore(season);
        assertThat(jdbc.queryForList("SELECT club_id,played,wins,draws,losses,goals_for,goals_against,points FROM club_season_record WHERE season_id=? ORDER BY club_id", season)).isEqualTo(first);
    }

    @Test
    void resultPublicationRecalculatesOnlyParticipantsAndReminderApiChangesAfterCommitWork() throws Exception {
        long season = season("P35赛果-" + System.nanoTime(), "IN_PROGRESS", 4);
        enroll(season, 0, 1);
        long round = round(season);
        long match = match(season, round, 0, 1, "IN_PROGRESS", null, null);
        long role = id("SELECT role_id FROM sys_role WHERE role_code='EVENT_ADMIN'");
        String suffix = Long.toString(System.nanoTime()).substring(5);
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,employee_no,role_id,user_status) VALUES(?,?,?,?,?,?,'ENABLED')",
                "P35_EVENT_" + suffix, "139" + suffix.substring(0, 8), "x", "P35赛事管理员", "E" + suffix.substring(0, 5), role);
        long secondAdmin = id("SELECT MAX(user_id) FROM sys_user");

        reminder(season).andExpect(jsonPath("$.data.total").value(1));
        results.submit(match, eventAdminId, new MatchScoreRequest(2, 1));
        reminder(season).andExpect(jsonPath("$.data.total").value(1));
        results.submit(match, secondAdmin, new MatchScoreRequest(2, 1));
        reminder(season).andExpect(jsonPath("$.data.total").value(0)).andExpect(jsonPath("$.data.records").isEmpty());

        assertStandingSetAndScore(season);
        mvc.perform(get("/api/admin/seasons/{id}/standings", season).header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].clubId", containsInAnyOrder((int) clubId(0), (int) clubId(1))));
    }

    private void assertStandingSetAndScore(long season) {
        assertThat(jdbc.queryForList("SELECT club_id FROM club_season_record WHERE season_id=? ORDER BY club_id", Long.class, season))
                .containsExactlyInAnyOrder(clubId(0), clubId(1));
        assertThat(jdbc.queryForObject("SELECT CONCAT(played,',',wins,',',draws,',',losses,',',goals_for,',',goals_against,',',points) FROM club_season_record WHERE season_id=? AND club_id=?", String.class, season, clubId(0)))
                .isEqualTo("1,1,0,0,2,1,3");
        assertThat(jdbc.queryForObject("SELECT CONCAT(played,',',wins,',',draws,',',losses,',',goals_for,',',goals_against,',',points) FROM club_season_record WHERE season_id=? AND club_id=?", String.class, season, clubId(1)))
                .isEqualTo("1,0,0,1,1,2,0");
    }

    private long season(String name, String status, int maxClubs) {
        LocalDateTime now = time.now();
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,?,?)",
                name, now.toLocalDate().minusMonths(1), now.toLocalDate().plusMonths(1), now.minusMonths(2), now.minusMonths(1), maxClubs, status);
        return id("SELECT MAX(season_id) FROM season_info");
    }

    private void enroll(long season, int... indexes) {
        for (int index : indexes) {
            jdbc.update("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(?,?,?,'SUBMITTED',?)",
                    season, clubId(index), stadiumId(index), time.now().minusMonths(1));
        }
    }

    private long round(long season) {
        LocalDateTime now = time.now();
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'P35轮次',?,?,'PUBLISHED')",
                season, now.toLocalDate().minusDays(2), now.toLocalDate().plusDays(2));
        return id("SELECT MAX(round_id) FROM round_info");
    }

    private long match(long season, long round, int home, int away, String status, Integer homeScore, Integer awayScore) {
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,home_score,away_score,match_status,published_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                season, round, clubId(home), clubId(away), stadiumId(home), time.now().minusHours(1), homeScore, awayScore, status, time.now().minusDays(1));
        return id("SELECT MAX(match_id) FROM match_info");
    }

    private org.springframework.test.web.servlet.ResultActions reminder(long season) throws Exception {
        return mvc.perform(get("/api/admin/matches/result-reminders?size=10&seasonId=" + season).header("Authorization", bearer(eventToken))).andExpect(status().isOk());
    }

    private String login(String phone) throws Exception {
        JsonNode body = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(TestLoginPayload.forPhone(phone, "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private long clubId(int index) { return ((Number) clubs.get(index).get("club_id")).longValue(); }
    private long stadiumId(int index) { return ((Number) clubs.get(index).get("home_stadium_id")).longValue(); }
    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private static String bearer(String token) { return "Bearer " + token; }
}
