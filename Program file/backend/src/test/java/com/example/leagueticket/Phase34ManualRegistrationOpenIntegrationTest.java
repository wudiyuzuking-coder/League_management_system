package com.example.leagueticket;

import com.example.leagueticket.service.SeasonLifecycleService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase34ManualRegistrationOpenIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemTimeService time;
    @Autowired SeasonLifecycleService lifecycle;

    private String eventToken;
    private String clubToken;
    private long clubId;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        String hash = passwordEncoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_club','demo_event_admin')", hash);
        clubId = jdbc.queryForObject("SELECT club_id FROM club_info WHERE home_stadium_id IS NOT NULL AND club_status='ACTIVE' ORDER BY club_id LIMIT 1", Long.class);
        jdbc.update("UPDATE sys_user SET club_id=? WHERE username='demo_club'", clubId);
        ensureRoster(clubId);
        eventToken = login("13800000005");
        clubToken = login("13800000003");
    }

    @AfterEach
    void tearDown() {
        cleanup();
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
    }

    @Test
    void manualOpenImmediatelyUpdatesWindowAndAllowsClubQueryAndEnrollment() throws Exception {
        LocalDateTime before = time.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime plannedStart = before.plusDays(6).withHour(20).withMinute(0).withSecond(0);
        LocalDateTime deadline = before.plusDays(21).withHour(19).withMinute(59).withSecond(0);
        long season = insertDraft("P34人工提前开启", plannedStart, deadline);

        JsonNode opened = body(mvc.perform(post("/api/admin/seasons/{id}/registration/open", season)
                        .header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seasonStatus").value("REGISTRATION"))
                .andReturn().getResponse().getContentAsString()).path("data");

        LocalDateTime actualStart = LocalDateTime.parse(opened.path("registrationStartTime").asText());
        assertThat(actualStart).isBefore(plannedStart);
        assertThat(actualStart).isBetween(before.minusSeconds(1), time.now().plusSeconds(1));
        assertThat(LocalDateTime.parse(opened.path("registrationDeadline").asText())).isEqualTo(deadline);
        assertThat(value("SELECT registration_start_time FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(actualStart);
        assertThat(value("SELECT registration_deadline FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(deadline);

        mvc.perform(get("/api/club/enrollments/available-seasons").header("Authorization", bearer(clubToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].seasonId", hasItem((int) season)));

        mvc.perform(post("/api/club/enrollments").header("Authorization", bearer(clubToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("seasonId", season))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seasonId").value((int) season));
    }

    @Test
    void repeatedManualOpenKeepsFirstActualStart() throws Exception {
        LocalDateTime now = time.now().truncatedTo(ChronoUnit.SECONDS);
        long season = insertDraft("P34重复开启", now.plusDays(5), now.plusDays(20));
        mvc.perform(post("/api/admin/seasons/{id}/registration/open", season).header("Authorization", bearer(eventToken)))
                .andExpect(status().isOk());
        LocalDateTime first = value("SELECT registration_start_time FROM season_info WHERE season_id=?", LocalDateTime.class, season);

        mvc.perform(post("/api/admin/seasons/{id}/registration/open", season).header("Authorization", bearer(eventToken)))
                .andExpect(status().isConflict());

        assertThat(value("SELECT registration_start_time FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(first);
    }

    @Test
    void manualOpenAtOrAfterDeadlineIsRejectedWithoutMutation() throws Exception {
        LocalDateTime now = time.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime plannedStart = now.minusDays(3);
        LocalDateTime deadline = now.minusSeconds(1);
        long season = insertDraft("P34截止后拒绝", plannedStart, deadline);

        mvc.perform(post("/api/admin/seasons/{id}/registration/open", season).header("Authorization", bearer(eventToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("报名截止时间已到，无法开启报名"));

        assertThat(value("SELECT season_status FROM season_info WHERE season_id=?", String.class, season)).isEqualTo("DRAFT");
        assertThat(value("SELECT registration_start_time FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(plannedStart);
        assertThat(value("SELECT registration_deadline FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(deadline);
    }

    @Test
    void delayedAutomaticOpenPreservesPlannedStart() {
        LocalDateTime now = time.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime plannedStart = now.minusMinutes(1);
        long season = insertDraft("P34自动计划时间", plannedStart, now.plusDays(15));

        assertThat(lifecycle.openRegistrationIfDue(season)).isTrue();

        assertThat(value("SELECT season_status FROM season_info WHERE season_id=?", String.class, season)).isEqualTo("REGISTRATION");
        assertThat(value("SELECT registration_start_time FROM season_info WHERE season_id=?", LocalDateTime.class, season)).isEqualTo(plannedStart);
    }

    private long insertDraft(String name, LocalDateTime registrationStart, LocalDateTime deadline) {
        LocalDate start = LocalDate.of(2035, 1, 1);
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,4,'DRAFT')",
                name, start, start.plusMonths(3), registrationStart, deadline);
        return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
    }

    private void ensureRoster(long club) {
        jdbc.update("UPDATE player_info SET player_status='INACTIVE',lineup_role=NULL WHERE club_id=?", club);
        jdbc.update("UPDATE coach_info SET coach_status='INACTIVE' WHERE club_id=?", club);
        for (int i = 0; i < 11; i++) {
            int shirt = 70 + i;
            jdbc.update("INSERT INTO player_info(club_id,player_name,shirt_no,position,nationality,birth_date,player_status,lineup_role) VALUES(?,?,?,?,?,'2000-01-01','ACTIVE','STARTER') ON DUPLICATE KEY UPDATE player_status='ACTIVE',birth_date='2000-01-01',position=VALUES(position),lineup_role='STARTER'",
                    club, "P34球员" + club + "-" + shirt, shirt,
                    i == 0 ? "GOALKEEPER" : i < 5 ? "DEFENDER" : i < 8 ? "MIDFIELDER" : "FORWARD", "中国");
        }
        jdbc.update("INSERT INTO coach_info(club_id,coach_name,title,coach_status) SELECT ?,?,'HEAD_COACH','ACTIVE' WHERE NOT EXISTS(SELECT 1 FROM coach_info WHERE club_id=? AND coach_name=?)",
                club, "P34教练" + club, club, "P34教练" + club);
        jdbc.update("UPDATE coach_info SET title='HEAD_COACH',coach_status='ACTIVE' WHERE club_id=? AND coach_name=?", club, "P34教练" + club);
    }

    private String login(String phone) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone(phone, "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private JsonNode body(String content) throws Exception {
        return json.readTree(content);
    }

    private <T> T value(String sql, Class<T> type, Object... args) {
        return jdbc.queryForObject(sql, type, args);
    }

    private void cleanup() {
        List<Long> seasons = jdbc.queryForList("SELECT season_id FROM season_info WHERE season_name LIKE 'P34%'", Long.class);
        for (Long season : seasons) {
            jdbc.update("DELETE FROM club_season_enrollment_player WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id=?)", season);
            jdbc.update("DELETE FROM club_season_enrollment_coach WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id=?)", season);
            jdbc.update("DELETE FROM club_season_enrollment WHERE season_id=?", season);
            jdbc.update("DELETE FROM season_info WHERE season_id=?", season);
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
