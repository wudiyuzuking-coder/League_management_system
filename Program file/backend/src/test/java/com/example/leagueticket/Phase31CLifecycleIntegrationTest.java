package com.example.leagueticket;

import com.example.leagueticket.task.MatchLifecycleTask;
import com.example.leagueticket.task.SeasonLifecycleTask;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.service.SeasonLifecycleService;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase31CLifecycleIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SystemTimeService time;
    @Autowired SeasonLifecycleTask seasonTask;
    @Autowired MatchLifecycleTask matchTask;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SeasonLifecycleService lifecycle;

    private final Map<Long, String> originalSeasonStatuses = new LinkedHashMap<>();
    private String eventAdmin;
    private long clubA;
    private long clubB;
    private long stadiumA;

    @BeforeEach
    void setup() throws Exception {
        cleanupPhaseData();
        jdbc.query("SELECT season_id,season_status FROM season_info", rs -> {
            originalSeasonStatuses.put(rs.getLong(1), rs.getString(2));
        });
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE phone='13800000005'", passwordEncoder.encode("123456"));
        var clubs = jdbc.queryForList("SELECT club_id,home_stadium_id FROM club_info WHERE home_stadium_id IS NOT NULL ORDER BY club_id LIMIT 2");
        clubA = ((Number) clubs.get(0).get("club_id")).longValue();
        stadiumA = ((Number) clubs.get(0).get("home_stadium_id")).longValue();
        clubB = ((Number) clubs.get(1).get("club_id")).longValue();
        eventAdmin = login();
    }

    @AfterEach
    void cleanup() {
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        cleanupPhaseData();
        originalSeasonStatuses.forEach((id, status) -> jdbc.update("UPDATE season_info SET season_status=? WHERE season_id=?", status, id));
        originalSeasonStatuses.clear();
    }

    @Test
    void draftRegistrationStartsAtExactConfiguredTimeAndIncompleteDraftStaysDraft() {
        long season = draftSeason("P31C自动报名", LocalDate.of(2048, 11, 15), true);
        long incomplete = draftSeason("P31C资料不完整", LocalDate.of(2048, 11, 15), false);

        setTime(LocalDateTime.of(2048, 10, 16, 19, 59));
        seasonTask.advanceSeasons();
        assertStatus(season, "DRAFT");

        setTime(LocalDateTime.of(2048, 10, 16, 20, 0));
        seasonTask.advanceSeasons();
        assertStatus(season, "REGISTRATION");
        assertStatus(incomplete, "DRAFT");

        seasonTask.advanceSeasons();
        assertStatus(season, "REGISTRATION");
    }

    @Test
    void manualAndAutomaticRegistrationRaceOnlyAdvancesOnce() throws Exception {
        long season = draftSeason("P31C并发报名", LocalDate.of(2048, 11, 15), true);
        setTime(LocalDateTime.of(2048, 10, 16, 20, 0));
        CyclicBarrier gate = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> manual = pool.submit(() -> {
                await(gate);
                try { lifecycle.openRegistration(season); } catch (RuntimeException ignored) { }
            });
            Future<?> automatic = pool.submit(() -> {
                await(gate);
                try { lifecycle.openRegistrationIfDue(season); } catch (RuntimeException ignored) { }
            });
            manual.get(10, TimeUnit.SECONDS);
            automatic.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertStatus(season, "REGISTRATION");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM season_info WHERE season_id=? AND season_status='REGISTRATION'", Integer.class, season)).isEqualTo(1);
    }

    @Test
    void preparingSeasonStartsAtMidnightAndManualEarlyStartIsRejected() throws Exception {
        long season = confirmedSeason("P31C自动赛季开始", LocalDate.of(2048, 11, 15), LocalDateTime.of(2048, 11, 15, 20, 0));

        setTime(LocalDateTime.of(2048, 11, 14, 23, 59));
        seasonTask.advanceSeasons();
        assertStatus(season, "PREPARING");
        seasonStatusRequest(season, "IN_PROGRESS").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("赛季将在开始日由系统自动进入进行中"));

        setTime(LocalDateTime.of(2048, 11, 15, 0, 0));
        seasonStatusRequest(season, "IN_PROGRESS").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("赛季将在开始日由系统自动进入进行中"));
        seasonTask.advanceSeasons();
        assertStatus(season, "IN_PROGRESS");
        seasonTask.advanceSeasons();
        assertStatus(season, "IN_PROGRESS");
    }

    @Test
    void publishedMatchStartsAtKickoffAndRepeatedScanHasNoEffect() throws Exception {
        long season = confirmedSeason("P31C自动比赛开始", LocalDate.of(2048, 11, 15), LocalDateTime.of(2048, 11, 15, 20, 0));
        long match = jdbc.queryForObject("SELECT match_id FROM match_info WHERE season_id=?", Long.class, season);

        setTime(LocalDateTime.of(2048, 11, 15, 19, 59));
        matchTask.startPublishedMatches();
        assertMatchStatus(match, "PUBLISHED");

        setTime(LocalDateTime.of(2048, 11, 15, 20, 0));
        matchTask.startPublishedMatches();
        assertMatchStatus(match, "IN_PROGRESS");
        matchTask.startPublishedMatches();
        assertMatchStatus(match, "IN_PROGRESS");
        mvc.perform(post("/api/admin/matches/{id}/result-submissions", match)
                        .header("Authorization", bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"homeScore\":2,\"awayScore\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reviewReason").value("SINGLE_SUBMISSION"));
    }

    @Test
    void eventAdminCannotPublishStartOrCancelMatches() throws Exception {
        long season = confirmedSeason("P31C人工比赛状态", LocalDate.of(2048, 11, 15), LocalDateTime.of(2048, 11, 15, 20, 0));
        long published = jdbc.queryForObject("SELECT match_id FROM match_info WHERE season_id=?", Long.class, season);
        long draft = insertMatch(season, roundId(season), LocalDateTime.of(2048, 11, 16, 20, 0), "DRAFT");

        matchStatus(draft, "PUBLISHED").andExpect(status().isConflict());
        matchStatus(published, "IN_PROGRESS").andExpect(status().isConflict());
        matchStatus(published, "CANCELLED").andExpect(status().isConflict());
        jdbc.update("UPDATE match_info SET match_status='IN_PROGRESS' WHERE match_id=?", published);
        matchStatus(published, "CANCELLED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("比赛发布、开始和取消均由系统自动维护"));
    }

    @Test
    void eventAdminTicketWriteEndpointsAreRemoved() throws Exception {
        String auth = bearer(eventAdmin);
        mvc.perform(post("/api/admin/matches/1/ticket-zones").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/admin/matches/1/ticketing/initialize-standard").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/match-ticket-zones/1").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/admin/match-ticket-zones/1/status").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/match-ticket-zones/1/inventory/generate").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/match-seat-inventory/1/status").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private long draftSeason(String name, LocalDate start, boolean complete) {
        if (complete) {
            jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,4,'DRAFT')",
                    name, start, start.plusMonths(2), start.minusDays(30).atTime(20, 0), start.minusDays(15).atTime(19, 59));
        } else {
            jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,season_status) VALUES(?,?,?,?,'DRAFT')",
                    name, start, start.plusMonths(2), start.minusDays(30).atTime(20, 0));
        }
        return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
    }

    private long confirmedSeason(String name, LocalDate start, LocalDateTime matchTime) {
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,2,'PREPARING')",
                name, start, start.plusMonths(2), start.minusDays(30).atTime(20, 0), start.minusDays(15).atTime(19, 59));
        long season = jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,?,?,?,'PUBLISHED')",
                season, name + "轮次", start, start.plusDays(7));
        long round = roundId(season);
        long match = insertMatch(season, round, matchTime, "PUBLISHED");
        jdbc.update("INSERT INTO season_schedule_batch(season_id,batch_status,trigger_type,club_count,round_count,match_count,generated_at,confirmed_at) VALUES(?,'CONFIRMED','MANUAL',2,1,1,?,?)",
                season, matchTime.minusMonths(1), matchTime.minusMonths(1));
        long batch = jdbc.queryForObject("SELECT batch_id FROM season_schedule_batch WHERE season_id=?", Long.class, season);
        jdbc.update("INSERT INTO season_schedule_match(batch_id,match_id) VALUES(?,?)", batch, match);
        return season;
    }

    private long insertMatch(long season, long round, LocalDateTime matchTime, String status) {
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(?,?,?,?,?,?,?,?)",
                season, round, clubA, clubB, stadiumA, matchTime, status, "PUBLISHED".equals(status) ? matchTime.minusMonths(1) : null);
        return jdbc.queryForObject("SELECT MAX(match_id) FROM match_info WHERE season_id=?", Long.class, season);
    }

    private long roundId(long season) {
        return jdbc.queryForObject("SELECT round_id FROM round_info WHERE season_id=?", Long.class, season);
    }

    private void setTime(LocalDateTime target) {
        // Offset is persisted with second precision. Round toward the target so an exact
        // boundary such as 20:00:00 is not observed a fraction of a second too early.
        long offset = Duration.between(time.realNow(), target).plusNanos(999_999_999).getSeconds();
        jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'", Long.toString(offset));
    }

    private void assertStatus(long season, String expected) {
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?", String.class, season)).isEqualTo(expected);
    }

    private void assertMatchStatus(long match, String expected) {
        assertThat(jdbc.queryForObject("SELECT match_status FROM match_info WHERE match_id=?", String.class, match)).isEqualTo(expected);
    }

    private org.springframework.test.web.servlet.ResultActions seasonStatusRequest(long season, String value) throws Exception {
        return mvc.perform(put("/api/admin/seasons/{id}/status", season).header("Authorization", bearer(eventAdmin))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("seasonStatus", value))));
    }

    private org.springframework.test.web.servlet.ResultActions matchStatus(long match, String value) throws Exception {
        return mvc.perform(put("/api/admin/matches/{id}/status", match).header("Authorization", bearer(eventAdmin))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("matchStatus", value))));
    }

    private String login() throws Exception {
        JsonNode body = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone("13800000005", "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private void cleanupPhaseData() {
        jdbc.update("DELETE FROM match_result_review WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%'))");
        jdbc.update("DELETE FROM match_result_submission WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%'))");
        jdbc.update("DELETE FROM season_schedule_match WHERE batch_id IN (SELECT batch_id FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%'))");
        jdbc.update("DELETE FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%')");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P31C%')");
        jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'P31C%'");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static void await(CyclicBarrier gate) {
        try {
            gate.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
