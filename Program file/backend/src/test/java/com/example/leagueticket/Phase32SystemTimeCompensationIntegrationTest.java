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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase32SystemTimeCompensationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemTimeService time;

    private String token;
    private final Map<Long, String> originalSeasonStatuses = new LinkedHashMap<>();
    private final Map<Long, String> originalMatchStatuses = new LinkedHashMap<>();
    private final Map<Long, LocalDateTime> originalPendingExpiries = new LinkedHashMap<>();

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        jdbc.query("SELECT season_id,season_status FROM season_info", rs -> { originalSeasonStatuses.put(rs.getLong(1), rs.getString(2)); });
        jdbc.query("SELECT match_id,match_status FROM match_info", rs -> { originalMatchStatuses.put(rs.getLong(1), rs.getString(2)); });
        jdbc.query("SELECT order_id,expire_time FROM ticket_order WHERE order_status='PENDING_PAYMENT'", rs -> { originalPendingExpiries.put(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime()); });
        jdbc.update("UPDATE season_info SET season_status='FINISHED'");
        jdbc.update("UPDATE match_info SET match_status='FINISHED'");
        jdbc.update("UPDATE ticket_order SET expire_time='2099-12-31 23:59:59' WHERE order_status='PENDING_PAYMENT'");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username='demo_admin'", passwordEncoder.encode("123456"));
        token = login();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");
        cleanup();
        originalSeasonStatuses.forEach((id, status) -> jdbc.update("UPDATE season_info SET season_status=? WHERE season_id=?", status, id));
        originalMatchStatuses.forEach((id, status) -> jdbc.update("UPDATE match_info SET match_status=? WHERE match_id=?", status, id));
        originalPendingExpiries.forEach((id, expiry) -> jdbc.update("UPDATE ticket_order SET expire_time=? WHERE order_id=?", expiry, id));
        originalSeasonStatuses.clear();
        originalMatchStatuses.clear();
        originalPendingExpiries.clear();
    }

    @Test
    void jumpingToDayMinus29OpensRegistrationImmediatelyAndResetDoesNotReverseState() throws Exception {
        LocalDateTime boundary = LocalDateTime.of(2048, 10, 16, 20, 0);
        long season = insertSeason("P32报名边界", "DRAFT", LocalDate.of(2048, 11, 15), boundary,
                LocalDateTime.of(2048, 10, 31, 19, 59));

        assertStatus(season, "DRAFT");

        setTime(boundary.plusDays(1).plusHours(3));
        assertStatus(season, "REGISTRATION");

        mvc.perform(post("/api/system-time/reset").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertStatus(season, "REGISTRATION");
    }

    @Test
    void oneForwardJumpClosesRegistrationPublishesAndStartsDueSeasonAndMatchIdempotently() throws Exception {
        LocalDate start = LocalDate.of(2049, 3, 1);
        LocalDateTime deadline = start.minusDays(1).atTime(19, 59);
        long season = insertSeason("P32跨阶段补偿", "DRAFT", start,
                start.minusDays(30).atTime(20, 0), deadline);
        addTeam(season, 1);
        addTeam(season, 2);
        LocalDateTime target = start.atTime(20, 1);

        setTime(target);

        assertStatus(season, "IN_PROGRESS");
        assertThat(number("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=? AND batch_status='CONFIRMED'", season)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=?", season)).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=? AND match_status='IN_PROGRESS'", season)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=? AND match_status='PUBLISHED'", season)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM club_season_record WHERE season_id=?", season)).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?", season)).isEqualTo(16);
        assertThat(number("SELECT COUNT(*) FROM match_seat_inventory i JOIN match_info m ON m.match_id=i.match_id WHERE m.season_id=?", season)).isEqualTo(16);

        setTime(target.plusSeconds(1));
        assertThat(number("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=?", season)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM match_info WHERE season_id=?", season)).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id=?", season)).isEqualTo(16);
    }

    @Test
    void jumpingPastPendingOrderExpiryClosesOrderAndReleasesInventoryBeforeResponse() throws Exception {
        Map<String, Object> seat = jdbc.queryForMap("SELECT i.inventory_id,i.match_id,i.match_zone_id,z.ticket_price,z.zone_name_snapshot,s.row_no,s.seat_no FROM match_seat_inventory i JOIN match_ticket_zone z ON z.match_zone_id=i.match_zone_id JOIN stadium_seat s ON s.stadium_seat_id=i.stadium_seat_id WHERE i.inventory_status='AVAILABLE' ORDER BY i.inventory_id LIMIT 1");
        long inventory = ((Number) seat.get("inventory_id")).longValue();
        long match = ((Number) seat.get("match_id")).longValue();
        long zone = ((Number) seat.get("match_zone_id")).longValue();
        long user = jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='demo_user'", Long.class);
        LocalDateTime expire = time.realNow().plusHours(1).truncatedTo(ChronoUnit.SECONDS);
        jdbc.update("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,created_at) VALUES('P32-TIMEOUT',?,?,?,?,?,'PENDING_PAYMENT',?,?)",
                user, match, zone, 1, seat.get("ticket_price"), expire, expire.minusMinutes(15));
        long order = jdbc.queryForObject("SELECT order_id FROM ticket_order WHERE order_no='P32-TIMEOUT'", Long.class);
        jdbc.update("UPDATE match_seat_inventory SET inventory_status='LOCKED',lock_order_id=?,locked_at=?,lock_expire_time=? WHERE inventory_id=?",
                order, expire.minusMinutes(15), expire, inventory);
        jdbc.update("INSERT INTO order_item(order_id,inventory_id,ticket_price,zone_name_snapshot,row_no_snapshot,seat_no_snapshot,item_status) VALUES(?,?,?,?,?,?,'LOCKED')",
                order, inventory, seat.get("ticket_price"), seat.get("zone_name_snapshot"), seat.get("row_no"), seat.get("seat_no"));

        setTime(expire.plusSeconds(1));

        assertThat(jdbc.queryForObject("SELECT order_status FROM ticket_order WHERE order_id=?", String.class, order)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM ticket_order WHERE order_id=?", String.class, order)).isEqualTo("PAYMENT_TIMEOUT");
        assertThat(jdbc.queryForObject("SELECT item_status FROM order_item WHERE order_id=?", String.class, order)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT inventory_status FROM match_seat_inventory WHERE inventory_id=?", String.class, inventory)).isEqualTo("AVAILABLE");
    }

    private long insertSeason(String name, String status, LocalDate start, LocalDateTime registrationStart,
                              LocalDateTime deadline) {
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,2,?)",
                name, start, start.plusMonths(2), registrationStart, deadline, status);
        return jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?", Long.class, name);
    }

    private void addTeam(long season, int index) {
        String suffix = season + "-" + index;
        String stadiumName = "P32场馆" + suffix;
        String clubName = "P32俱乐部" + suffix;
        jdbc.update("INSERT INTO stadium_info(stadium_name,city,address,capacity,venue_model,stadium_status) VALUES(?,?,?,8,'STANDARD_8','ACTIVE')",
                stadiumName, "测试城", "测试路" + index);
        long stadium = jdbc.queryForObject("SELECT stadium_id FROM stadium_info WHERE stadium_name=?", Long.class, stadiumName);
        jdbc.update("INSERT INTO club_info(club_name,home_city,home_stadium_id,club_status) VALUES(?,?,?,'ACTIVE')",
                clubName, "测试城", stadium);
        long club = jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name=?", Long.class, clubName);
        jdbc.update("INSERT INTO club_home_stadium_config(club_id,stadium_id,rows_per_zone,long_side_seats_per_row,short_side_seats_per_row,vip_price,normal_price) VALUES(?,?,1,1,1,200,100)", club, stadium);
        String[] directions = {"EAST", "WEST", "SOUTH", "NORTH"};
        String[] types = {"VIP", "NORMAL"};
        int sort = 0;
        for (String direction : directions) for (String type : types) {
            String code = direction + "_" + type;
            jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,zone_direction,ticket_type,sort_order,zone_status) VALUES(?,?,?,?,?,?,'ACTIVE')",
                    stadium, code, code, direction, type, ++sort);
            long zone = jdbc.queryForObject("SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id=? AND zone_code=?", Long.class, stadium, code);
            jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'A',1,'1',1,0,'ACTIVE')", stadium, zone);
        }
        jdbc.update("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(?,?,?,'SUBMITTED',?)",
                season, club, stadium, time.now());
    }

    private void setTime(LocalDateTime target) throws Exception {
        mvc.perform(put("/api/system-time").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("targetTime", target.truncatedTo(ChronoUnit.SECONDS)))))
                .andExpect(status().isOk());
    }

    private String login() throws Exception {
        JsonNode body = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone("13800000002", "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private void assertStatus(long season, String expected) {
        assertThat(jdbc.queryForObject("SELECT season_status FROM season_info WHERE season_id=?", String.class, season)).isEqualTo(expected);
    }

    private int number(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE order_no LIKE 'P32-%')");
        jdbc.update("UPDATE match_seat_inventory SET inventory_status='AVAILABLE',lock_order_id=NULL,locked_at=NULL,lock_expire_time=NULL WHERE lock_order_id IN (SELECT order_id FROM ticket_order WHERE order_no LIKE 'P32-%')");
        jdbc.update("DELETE FROM ticket_order WHERE order_no LIKE 'P32-%'");
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%'))");
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%'))");
        jdbc.update("DELETE FROM season_schedule_match WHERE batch_id IN (SELECT batch_id FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%'))");
        jdbc.update("DELETE FROM season_schedule_batch WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%')");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%')");
        jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%')");
        jdbc.update("DELETE FROM club_season_enrollment WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'P32%')");
        jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'P32%'");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'P32%')");
        jdbc.update("DELETE FROM stadium_zone WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'P32%')");
        jdbc.update("DELETE FROM club_home_stadium_config WHERE club_id IN (SELECT club_id FROM club_info WHERE club_name LIKE 'P32%')");
        jdbc.update("DELETE FROM club_info WHERE club_name LIKE 'P32%'");
        jdbc.update("DELETE FROM stadium_info WHERE stadium_name LIKE 'P32%'");
    }

    private static String bearer(String value) {
        return "Bearer " + value;
    }
}
