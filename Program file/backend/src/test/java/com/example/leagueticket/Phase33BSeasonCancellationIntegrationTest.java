package com.example.leagueticket;

import com.example.leagueticket.service.ClubSeasonEnrollmentService;
import com.example.leagueticket.service.SeasonRegistrationDeadlineService;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
class Phase33BSeasonCancellationIntegrationTest {
    private static final String REASON = "参赛俱乐部不足 2 支";
    private static final String MESSAGE = "因参赛俱乐部不足 2 支，该赛季已取消";

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemTimeService time;
    @Autowired SeasonRegistrationDeadlineService deadlineService;
    @Autowired ClubSeasonEnrollmentService enrollmentService;

    private final Map<Long, SeasonSnapshot> originalSeasons = new LinkedHashMap<>();
    private final Map<Long, String> originalMatches = new LinkedHashMap<>();
    private final Map<Long, LocalDateTime> originalPendingExpiries = new LinkedHashMap<>();
    private final Map<String, UserSnapshot> originalUsers = new LinkedHashMap<>();
    private long clubA;
    private long clubB;
    private long stadiumA;
    private long stadiumB;
    private String eventAdmin;
    private String clubAToken;
    private String clubBToken;
    private String userToken;

    @BeforeEach
    void setup() throws Exception {
        cleanupPhaseData();
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        jdbc.query("SELECT season_id,season_status,cancel_reason,cancelled_at FROM season_info", rs -> {
            originalSeasons.put(rs.getLong(1), new SeasonSnapshot(rs.getString(2), rs.getString(3),
                    rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toLocalDateTime()));
        });
        jdbc.query("SELECT match_id,match_status FROM match_info", rs -> {
            originalMatches.put(rs.getLong(1), rs.getString(2));
        });
        jdbc.query("SELECT order_id,expire_time FROM ticket_order WHERE order_status='PENDING_PAYMENT'", rs -> {
            originalPendingExpiries.put(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime());
        });
        jdbc.update("UPDATE season_info SET season_status='FINISHED',cancel_reason=NULL,cancelled_at=NULL");
        jdbc.update("UPDATE match_info SET match_status='FINISHED'");
        jdbc.update("UPDATE ticket_order SET expire_time='2099-12-31 23:59:59' WHERE order_status='PENDING_PAYMENT'");

        var clubs = jdbc.queryForList("SELECT club_id,home_stadium_id FROM club_info WHERE club_name IN ('杭州潮汐足球俱乐部','苏州园林足球俱乐部') ORDER BY club_name");
        assertThat(clubs).hasSize(2);
        clubA = ((Number) clubs.get(0).get("club_id")).longValue();
        stadiumA = ((Number) clubs.get(0).get("home_stadium_id")).longValue();
        clubB = ((Number) clubs.get(1).get("club_id")).longValue();
        stadiumB = ((Number) clubs.get(1).get("home_stadium_id")).longValue();

        prepareLogin("13800000005", "EVENT_ADMIN");
        prepareLogin(phoneForClub(clubA), "CLUB");
        prepareLogin(phoneForClub(clubB), "CLUB");
        prepareLogin("13800000001", "USER");
        eventAdmin = login("13800000005", "EVENT_ADMIN", "0001");
        clubAToken = login(phoneForClub(clubA), "CLUB", null);
        clubBToken = login(phoneForClub(clubB), "CLUB", null);
        userToken = login("13800000001", "USER", null);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        cleanupPhaseData();
        originalSeasons.forEach((id, value) -> jdbc.update(
                "UPDATE season_info SET season_status=?,cancel_reason=?,cancelled_at=? WHERE season_id=?",
                value.status(), value.reason(), value.cancelledAt(), id));
        originalMatches.forEach((id, status) -> jdbc.update("UPDATE match_info SET match_status=? WHERE match_id=?", status, id));
        originalPendingExpiries.forEach((id, expiry) -> jdbc.update("UPDATE ticket_order SET expire_time=? WHERE order_id=?", expiry, id));
        originalUsers.forEach((key, value) -> jdbc.update(
                "UPDATE sys_user SET password_hash=?,last_login_at=? WHERE user_id=?",
                value.passwordHash(), value.lastLoginAt(), value.userId()));
        originalSeasons.clear();
        originalMatches.clear();
        originalPendingExpiries.clear();
        originalUsers.clear();
    }

    @Test
    void zeroAndOneSubmittedClubCancelWithoutCreatingCompetitionDataAndAreIdempotent() {
        LocalDateTime target = LocalDateTime.of(2051, 1, 10, 20, 0);
        setTimeDirect(target);
        long zero = insertSeason("P33零队取消", "REGISTRATION", target.minusDays(30).toLocalDate(), target.minusDays(1));
        long one = insertSeason("P33一队取消", "REGISTRATION", target.minusDays(30).toLocalDate(), target.minusDays(1));
        addEnrollment(one, clubA, stadiumA);

        assertThat(deadlineService.processRegistrationDeadline(zero)).isTrue();
        assertThat(deadlineService.processRegistrationDeadline(one)).isTrue();
        assertCancelled(zero, 0);
        assertCancelled(one, 1);
        LocalDateTime firstCancelledAt = jdbc.queryForObject("SELECT cancelled_at FROM season_info WHERE season_id=?", LocalDateTime.class, one);
        assertThat(jdbc.queryForObject("SELECT message FROM club_season_notification WHERE season_id=?", String.class, one)).isEqualTo(MESSAGE);

        assertThat(deadlineService.processRegistrationDeadline(one)).isFalse();
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", one)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cancelled_at FROM season_info WHERE season_id=?", LocalDateTime.class, one)).isEqualTo(firstCancelledAt);
    }

    @Test
    void twoSubmittedClubsContinueThroughExistingSchedulePublicationPath() {
        LocalDateTime target = LocalDateTime.of(2051, 3, 1, 20, 0);
        setTimeDirect(target);
        long season = insertSeason("P33两队排赛", "REGISTRATION", target.plusDays(20).toLocalDate(), target.minusMinutes(1));
        addEnrollment(season, clubA, stadiumA);
        addEnrollment(season, clubB, stadiumB);

        assertThat(deadlineService.processRegistrationDeadline(season)).isTrue();

        assertStatus(season, "PREPARING");
        assertThat(number("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=? AND batch_status='CONFIRMED'", season)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM round_info WHERE season_id=?", season)).isPositive();
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=?", season)).isPositive();
        assertThat(number("SELECT COUNT(*) FROM club_season_record WHERE season_id=?", season)).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?", season)).isPositive();
        assertThat(number("SELECT COUNT(*) FROM match_seat_inventory i JOIN match_info m ON m.match_id=i.match_id WHERE m.season_id=?", season)).isPositive();
        Map<String, Object> cancellation = jdbc.queryForMap("SELECT cancel_reason,cancelled_at FROM season_info WHERE season_id=?", season);
        assertThat(cancellation.get("cancel_reason")).isNull();
        assertThat(cancellation.get("cancelled_at")).isNull();
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", season)).isZero();
    }

    @Test
    void manualEarlyCloseStillConflictsAndCancelledSeasonRejectsWritesAndPublicReads() throws Exception {
        LocalDateTime target = LocalDateTime.of(2051, 5, 1, 12, 0);
        setTimeDirect(target);
        long early = insertSeason("P33人工不足", "REGISTRATION", target.plusMonths(2).toLocalDate(), target.plusDays(5));
        addEnrollment(early, clubA, stadiumA);
        mvc.perform(post("/api/admin/seasons/{id}/registration/close", early).header("Authorization", bearer(eventAdmin)))
                .andExpect(status().isConflict());
        assertStatus(early, "REGISTRATION");
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", early)).isZero();

        long cancelled = insertSeason("P33只读取消", "REGISTRATION", target.plusMonths(1).toLocalDate(), target.minusMinutes(1));
        addEnrollment(cancelled, clubA, stadiumA);
        deadlineService.processRegistrationDeadline(cancelled);
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'只读测试轮',?,?,'DRAFT')",
                cancelled, target.plusMonths(1).toLocalDate(), target.plusMonths(1).plusDays(1).toLocalDate());
        long round = jdbc.queryForObject("SELECT round_id FROM round_info WHERE season_id=?", Long.class, cancelled);
        jdbc.update("INSERT INTO club_season_record(season_id,club_id) VALUES(?,?)", cancelled, clubA);
        long record = jdbc.queryForObject("SELECT record_id FROM club_season_record WHERE season_id=?", Long.class, cancelled);
        String roundBody = json.writeValueAsString(Map.of("roundNo", 2, "roundName", "新轮次", "startDate", target.plusMonths(1).toLocalDate(), "endDate", target.plusMonths(1).plusDays(1).toLocalDate()));
        String auth = bearer(eventAdmin);
        mvc.perform(post("/api/admin/seasons/{id}/rounds", cancelled).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(roundBody)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/rounds/{id}", round).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(roundBody)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/rounds/{id}/status", round).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{\"roundStatus\":\"PUBLISHED\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/seasons/{id}/standings/init", cancelled).header("Authorization", auth)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/season-records/{id}", record).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{\"wins\":1,\"draws\":0,\"losses\":0,\"goalsFor\":1,\"goalsAgainst\":0}")).andExpect(status().isConflict());

        String userAuth = bearer(userToken);
        mvc.perform(get("/api/seasons/{id}", cancelled).header("Authorization", userAuth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/seasons/{id}/rounds", cancelled).header("Authorization", userAuth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/seasons/{id}/standings", cancelled).header("Authorization", userAuth)).andExpect(status().isNotFound());
    }

    @Test
    void notificationScopeEnrollmentHistoryAndCancelledConflictReleaseAreCorrect() throws Exception {
        LocalDateTime target = LocalDateTime.of(2051, 7, 1, 12, 0);
        setTimeDirect(target);
        long cancelled = insertSeason("P33通知取消", "REGISTRATION", target.plusMonths(2).toLocalDate(), target.minusMinutes(1));
        long enrollment = addEnrollment(cancelled, clubA, stadiumA);
        deadlineService.processRegistrationDeadline(cancelled);

        mvc.perform(get("/api/club/season-notifications").header("Authorization", bearer(clubAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seasonId").value(cancelled))
                .andExpect(jsonPath("$.data[0].seasonStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].message").value(MESSAGE));
        mvc.perform(get("/api/club/season-notifications").header("Authorization", bearer(clubBToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/club/season-notifications").header("Authorization", bearer(userToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/club/season-notifications").header("Authorization", bearer(eventAdmin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/club/enrollments/{id}", enrollment).header("Authorization", bearer(clubAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollmentStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.seasonStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value(REASON))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        long next = insertSeason("P33冲突释放", "REGISTRATION", target.plusMonths(2).plusDays(10).toLocalDate(), target.plusDays(1));
        assertThat(enrollmentService.availableSeasons(clubA).stream().anyMatch(row -> row.getSeasonId().equals(next))).isTrue();
        assertThat(enrollmentService.submit(clubA, new com.example.leagueticket.dto.EnrollmentRequest(next)).getEnrollmentStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void phase32ForwardJumpCancelsInOnePipelinePassAndBackwardTimeDoesNotReverseIt() throws Exception {
        LocalDate start = LocalDate.of(2052, 1, 20);
        LocalDateTime beforeRegistration = start.minusDays(31).atTime(12, 0);
        setTimeDirect(beforeRegistration);
        long season = insertSeasonWithRegistrationStart("P33跨阶段取消", start, start.minusDays(30).atTime(20, 0), start.minusDays(10).atTime(20, 0));
        addEnrollment(season, clubA, stadiumA);

        setTimeApi(start.plusDays(1).atTime(12, 0));
        assertStatus(season, "CANCELLED");
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", season)).isEqualTo(1);

        setTimeApi(start.plusDays(1).atTime(12, 0));
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", season)).isEqualTo(1);
        setTimeApi(beforeRegistration);
        assertStatus(season, "CANCELLED");
    }

    private long insertSeason(String name, String status, LocalDate start, LocalDateTime deadline) {
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,4,?)",
                name, start, start.plusMonths(3), deadline.minusMonths(1), deadline, status);
        return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
    }

    private long insertSeasonWithRegistrationStart(String name, LocalDate start, LocalDateTime registrationStart, LocalDateTime deadline) {
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,4,'DRAFT')",
                name, start, start.plusMonths(3), registrationStart, deadline);
        return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
    }

    private long addEnrollment(long season, long club, long stadium) {
        jdbc.update("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(?,?,?,'SUBMITTED',?)",
                season, club, stadium, time.now());
        return jdbc.queryForObject("SELECT enrollment_id FROM club_season_enrollment WHERE season_id=? AND club_id=?", Long.class, season, club);
    }

    private void assertCancelled(long season, int notifications) {
        Map<String, Object> row = jdbc.queryForMap("SELECT season_status,cancel_reason,cancelled_at FROM season_info WHERE season_id=?", season);
        assertThat(row.get("season_status")).isEqualTo("CANCELLED");
        assertThat(row.get("cancel_reason")).isEqualTo(REASON);
        assertThat(row.get("cancelled_at")).isNotNull();
        assertThat(number("SELECT COUNT(*) FROM club_season_notification WHERE season_id=?", season)).isEqualTo(notifications);
        assertThat(number("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?", season)).isZero();
        assertThat(number("SELECT COUNT(*) FROM round_info WHERE season_id=?", season)).isZero();
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=?", season)).isZero();
        assertThat(number("SELECT COUNT(*) FROM club_season_record WHERE season_id=?", season)).isZero();
        assertThat(number("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?", season)).isZero();
        assertThat(number("SELECT COUNT(*) FROM match_seat_inventory i JOIN match_info m ON m.match_id=i.match_id WHERE m.season_id=?", season)).isZero();
    }

    private void prepareLogin(String phone, String role) {
        Map<String, Object> row = jdbc.queryForMap("SELECT u.user_id,u.password_hash,u.last_login_at FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE u.phone=? AND r.role_code=?", phone, role);
        long userId = ((Number) row.get("user_id")).longValue();
        originalUsers.put(phone + ":" + role, new UserSnapshot(userId, (String) row.get("password_hash"),
                asLocalDateTime(row.get("last_login_at"))));
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE user_id=?", passwordEncoder.encode("123456"), userId);
    }

    private String phoneForClub(long clubId) {
        return jdbc.queryForObject("SELECT u.phone FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE u.club_id=? AND r.role_code='CLUB'", String.class, clubId);
    }

    private String login(String phone, String role, String employeeNo) throws Exception {
        JsonNode body = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forRole(phone, "123456", role, employeeNo))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private void setTimeApi(LocalDateTime target) throws Exception {
        mvc.perform(put("/api/system-time").header("Authorization", bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("targetTime", target.truncatedTo(ChronoUnit.SECONDS)))))
                .andExpect(status().isOk());
    }

    private void setTimeDirect(LocalDateTime target) {
        long offset = Duration.between(time.realNow(), target).plusNanos(999_999_999).getSeconds();
        jdbc.update("UPDATE sys_config SET config_value=? WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'", Long.toString(offset));
    }

    private void assertStatus(long season, String expected) {
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?", String.class, season)).isEqualTo(expected);
    }

    private int number(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime dateTime) return dateTime;
        return ((java.sql.Timestamp) value).toLocalDateTime();
    }

    private void cleanupPhaseData() {
        String seasonIds = "SELECT season_id FROM season_info WHERE season_name LIKE 'P33%'";
        jdbc.update("DELETE FROM club_season_notification WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (" + seasonIds + "))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (" + seasonIds + "))");
        jdbc.update("DELETE FROM season_schedule_match WHERE batch_id IN (SELECT batch_id FROM season_schedule_batch WHERE season_id IN (" + seasonIds + "))");
        jdbc.update("DELETE FROM season_schedule_batch WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM club_season_record WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM club_season_enrollment_player WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id IN (" + seasonIds + "))");
        jdbc.update("DELETE FROM club_season_enrollment_coach WHERE enrollment_id IN (SELECT enrollment_id FROM club_season_enrollment WHERE season_id IN (" + seasonIds + "))");
        jdbc.update("DELETE FROM club_season_enrollment WHERE season_id IN (" + seasonIds + ")");
        jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'P33%'");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record SeasonSnapshot(String status, String reason, LocalDateTime cancelledAt) { }
    private record UserSnapshot(long userId, String passwordHash, LocalDateTime lastLoginAt) { }
}
