package com.example.leagueticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class StatisticsIntegrationTest {
    private static final String PREFIX = "IT14";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    long seasonId, matchA, matchB, emptyMatch, clubA, clubB;
    String adminToken, clubToken, userToken, checkerToken;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_admin','demo_club','demo_checker')", encoder.encode("123456"));
        long adminId = id("SELECT user_id FROM sys_user WHERE username='demo_admin'");
        long userId = id("SELECT user_id FROM sys_user WHERE username='demo_user'");
        long checkerId = id("SELECT user_id FROM sys_user WHERE username='demo_checker'");
        clubA = id("SELECT club_id FROM sys_user WHERE username='demo_club'");
        clubB = id("SELECT club_id FROM club_info WHERE club_id<>" + clubA + " ORDER BY club_id LIMIT 1");
        long stadiumId = id("SELECT stadium_id FROM stadium_info ORDER BY stadium_id LIMIT 1");

        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT14统计赛季','2026-01-01','2026-12-31','ACTIVE')");
        seasonId = id("SELECT season_id FROM season_info WHERE season_name='IT14统计赛季'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,99,'IT14统计轮次','2026-09-01','2026-09-30','PUBLISHED')", seasonId);
        long roundId = id("SELECT round_id FROM round_info WHERE season_id=" + seasonId);
        matchA = match(roundId, clubA, clubB, stadiumId, "2026-09-10 19:30:00");
        matchB = match(roundId, clubB, clubA, stadiumId, "2026-09-11 19:30:00");
        emptyMatch = match(roundId, clubA, clubB, stadiumId, "2026-09-12 19:30:00");

        jdbc.update("INSERT INTO stadium_zone(stadium_id,zone_code,zone_name,sort_order,zone_status) VALUES(?,'IT14','IT14统计区',140,'ACTIVE')", stadiumId);
        long stadiumZoneId = id("SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id=" + stadiumId + " AND zone_code='IT14'");
        for (int seat = 1; seat <= 10; seat++) {
            jdbc.update("INSERT INTO stadium_seat(stadium_id,stadium_zone_id,row_no,row_seq,seat_no,seat_seq,center_distance,seat_status) VALUES(?,?,'1排',1,?,?,0,'ACTIVE')", stadiumId, stadiumZoneId, seat + "座", seat);
        }
        long zoneA = matchZone(matchA, stadiumZoneId, adminId, new BigDecimal("100.00"));
        long zoneB = matchZone(matchB, stadiumZoneId, adminId, new BigDecimal("50.00"));
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,CASE WHEN seat_seq<=8 THEN 'SOLD' ELSE 'AVAILABLE' END FROM stadium_seat WHERE stadium_zone_id=?", matchA, zoneA, stadiumZoneId);
        jdbc.update("INSERT INTO match_seat_inventory(match_id,match_zone_id,stadium_seat_id,inventory_status) SELECT ?,?,stadium_seat_id,CASE WHEN seat_seq<=2 THEN 'SOLD' ELSE 'AVAILABLE' END FROM stadium_seat WHERE stadium_zone_id=? AND seat_seq<=5", matchB, zoneB, stadiumZoneId);

        long paidA1 = order(userId, matchA, zoneA, 4, "400.00", "PAID", "A1");
        long paidA2 = order(userId, matchA, zoneA, 4, "400.00", "PAID", "A2");
        long refundedA = order(userId, matchA, zoneA, 2, "200.00", "REFUNDED", "AR");
        long paidB = order(userId, matchB, zoneB, 2, "100.00", "PAID", "B1");
        addItemsAndTickets(paidA1, matchA, 1, 4, "PAID", 4, "A1");
        addItemsAndTickets(paidA2, matchA, 5, 8, "PAID", 2, "A2");
        addItemsAndTickets(refundedA, matchA, 1, 2, "REFUNDED", 0, "AR");
        addItemsAndTickets(paidB, matchB, 1, 2, "PAID", 1, "B1");
        payment(paidA1, "400.00", "A1", "2026-08-20 10:00:00");
        payment(paidA2, "400.00", "A2", "2026-08-20 11:00:00");
        payment(refundedA, "200.00", "AR", "2026-08-20 12:00:00");
        payment(paidB, "100.00", "B1", "2026-08-21 10:00:00");
        jdbc.update("INSERT INTO refund_apply(refund_no,order_id,applicant_id,reason,refund_amount,refund_status,auditor_id,audit_remark,audit_time,created_at) VALUES('IT14-R-AR',?,?, '统计退票',200.00,'APPROVED',?,'通过','2026-08-20 15:00:00','2026-08-19 15:00:00')", refundedA, userId, adminId);
        addOtherStatusRefunds(userId, adminId);

        insertCheckins(matchA, checkerId, 6);
        insertCheckins(matchB, checkerId, 1);
        for (String result : new String[]{"CODE_NOT_FOUND", "WRONG_MATCH", "ORDER_INVALID", "TICKET_USED", "TICKET_REFUNDED", "TICKET_VOID"}) {
            jdbc.update("INSERT INTO checkin_record(match_id,ticket_id,scanned_ticket_code,checker_id,check_result,check_time,remark) VALUES(?,NULL,?,?,?,'2026-09-10 20:00:00','IT14统计异常')", matchA, "IT14-" + result, checkerId, result);
        }
        adminToken = login("demo_admin"); clubToken = login("demo_club"); userToken = login("demo_user"); checkerToken = login("demo_checker");
    }

    @AfterEach void after() { cleanup(); }

    @Test
    void overviewAndMatchStatisticsUseConfirmedDefinitions() throws Exception {
        mvc.perform(get("/api/admin/statistics/overview").queryParam("seasonId", String.valueOf(seasonId)).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalMatches").value(3))
                .andExpect(jsonPath("$.data.totalOrders").value(4)).andExpect(jsonPath("$.data.paidOrders").value(3))
                .andExpect(jsonPath("$.data.refundedOrders").value(1)).andExpect(jsonPath("$.data.validTicketsSold").value(10))
                .andExpect(jsonPath("$.data.checkedInTickets").value(7)).andExpect(jsonPath("$.data.grossSalesAmount").value(1100.00))
                .andExpect(jsonPath("$.data.refundAmount").value(200.00)).andExpect(jsonPath("$.data.netSalesAmount").value(900.00))
                .andExpect(jsonPath("$.data.averageAttendanceRate").value(26.67));
        mvc.perform(get("/api/admin/statistics/matches/{id}", matchA).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(10))
                .andExpect(jsonPath("$.data.validSoldCount").value(8)).andExpect(jsonPath("$.data.refundedCount").value(2))
                .andExpect(jsonPath("$.data.checkedInCount").value(6)).andExpect(jsonPath("$.data.ticketSaleRate").value(80.00))
                .andExpect(jsonPath("$.data.attendanceRate").value(60.00)).andExpect(jsonPath("$.data.netSalesAmount").value(800.00))
                .andExpect(jsonPath("$.data.zones[0].validSoldCount").value(8));
    }

    @Test
    void popularTrendRefundAndCheckinAggregationsAreExact() throws Exception {
        mvc.perform(get("/api/admin/statistics/popular-matches").queryParam("seasonId", String.valueOf(seasonId)).queryParam("limit", "3").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].matchId").value(matchA)).andExpect(jsonPath("$.data[1].matchId").value(matchB));
        mvc.perform(get("/api/admin/statistics/sales-trend").queryParam("seasonId", String.valueOf(seasonId)).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].statDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data[0].grossSalesAmount").value(1000.00)).andExpect(jsonPath("$.data[0].refundAmount").value(200.00))
                .andExpect(jsonPath("$.data[0].netSalesAmount").value(800.00)).andExpect(jsonPath("$.data[0].ticketsSold").value(10))
                .andExpect(jsonPath("$.data[1].grossSalesAmount").value(100.00)).andExpect(jsonPath("$.data[1].ticketsSold").value(2));
        mvc.perform(get("/api/admin/statistics/refunds").queryParam("startTime", "2026-08-01T00:00:00").queryParam("endTime", "2026-08-31T23:59:59").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(3)).andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.approvedCount").value(1)).andExpect(jsonPath("$.data.rejectedCount").value(1))
                .andExpect(jsonPath("$.data.approvedRefundAmount").value(200.00)).andExpect(jsonPath("$.data.refundRate").value(16.67));
        mvc.perform(get("/api/admin/statistics/checkins").queryParam("seasonId", String.valueOf(seasonId)).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAttempts").value(13)).andExpect(jsonPath("$.data.successCount").value(7))
                .andExpect(jsonPath("$.data.failedCount").value(6)).andExpect(jsonPath("$.data.successRate").value(53.85))
                .andExpect(jsonPath("$.data.codeNotFoundCount").value(1)).andExpect(jsonPath("$.data.wrongMatchCount").value(1))
                .andExpect(jsonPath("$.data.orderInvalidCount").value(1)).andExpect(jsonPath("$.data.ticketUsedCount").value(1))
                .andExpect(jsonPath("$.data.ticketRefundedCount").value(1)).andExpect(jsonPath("$.data.ticketVoidCount").value(1));
    }

    @Test
    void clubScopeIsForcedAndOtherRolesAreDenied() throws Exception {
        mvc.perform(get("/api/club/statistics/overview").queryParam("seasonId", String.valueOf(seasonId)).queryParam("clubId", String.valueOf(clubB)).header("Authorization", bearer(clubToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubA)).andExpect(jsonPath("$.data.homeMatchCount").value(2))
                .andExpect(jsonPath("$.data.validTicketsSold").value(8)).andExpect(jsonPath("$.data.netSalesAmount").value(800.00));
        String body = mvc.perform(get("/api/club/statistics/matches").queryParam("seasonId", String.valueOf(seasonId)).queryParam("clubId", String.valueOf(clubB)).header("Authorization", bearer(clubToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2)).andReturn().getResponse().getContentAsString();
        for (JsonNode row : json.readTree(body).path("data").path("records")) assertThat(row.path("homeClubId").asLong()).isEqualTo(clubA);
        mvc.perform(get("/api/admin/statistics/overview").header("Authorization", bearer(userToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/statistics/overview").header("Authorization", bearer(checkerToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/statistics/overview").header("Authorization", bearer(clubToken))).andExpect(status().isForbidden());
    }

    @Test
    void zeroDenominatorsAndPaginationDoNotFail() throws Exception {
        mvc.perform(get("/api/admin/statistics/matches/{id}", emptyMatch).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalSeatCount").value(0))
                .andExpect(jsonPath("$.data.ticketSaleRate").value(0.00)).andExpect(jsonPath("$.data.attendanceRate").value(0.00));
        mvc.perform(get("/api/admin/statistics/matches").queryParam("seasonId", String.valueOf(seasonId)).queryParam("page", "2").queryParam("size", "2").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.records.length()").value(1));
        mvc.perform(get("/api/admin/statistics/refunds").queryParam("seasonId", String.valueOf(seasonId)).queryParam("startTime", "2035-01-01T00:00:00").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(0))
                .andExpect(jsonPath("$.data.successfulPaidOrders").value(0)).andExpect(jsonPath("$.data.refundRate").value(0.00));
        mvc.perform(get("/api/admin/statistics/checkins").queryParam("seasonId", String.valueOf(seasonId)).queryParam("startTime", "2035-01-01T00:00:00").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAttempts").value(0))
                .andExpect(jsonPath("$.data.successCount").value(0)).andExpect(jsonPath("$.data.successRate").value(0.00));
    }

    private long match(long round, long home, long away, long stadium, String time) {
        jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(?,?,?,?,?,?,'PUBLISHED','2026-08-01 10:00:00')", seasonId, round, home, away, stadium, time);
        return id("SELECT match_id FROM match_info WHERE season_id=" + seasonId + " ORDER BY match_id DESC LIMIT 1");
    }
    private long matchZone(long match, long stadiumZone, long admin, BigDecimal price) {
        jdbc.update("INSERT INTO match_ticket_zone(match_id,stadium_zone_id,created_by,zone_name_snapshot,ticket_price,zone_status,sale_start_time,sale_end_time) VALUES(?,?,?,'IT14统计区',?,'CLOSED','2026-08-01','2026-09-01')", match, stadiumZone, admin, price);
        return id("SELECT match_zone_id FROM match_ticket_zone WHERE match_id=" + match);
    }
    private long order(long user, long match, long zone, int count, String amount, String status, String suffix) {
        jdbc.update("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,paid_at) VALUES(?,?,?,?,?,?,?, '2026-08-20 10:15:00',CASE WHEN ?='PAID' OR ?='REFUNDED' THEN '2026-08-20 10:00:00' ELSE NULL END)", PREFIX + "-O-" + suffix, user, match, zone, count, new BigDecimal(amount), status, status, status);
        return id("SELECT order_id FROM ticket_order WHERE order_no='" + PREFIX + "-O-" + suffix + "'");
    }
    private void addItemsAndTickets(long order, long match, int first, int last, String itemStatus, int usedCount, String suffix) {
        var inventories = jdbc.queryForList("SELECT inventory_id FROM match_seat_inventory WHERE match_id=? ORDER BY inventory_id LIMIT ? OFFSET ?", Long.class, match, last - first + 1, first - 1);
        int n = 0;
        for (Long inventory : inventories) {
            n++;
            BigDecimal price = itemStatus.equals("REFUNDED") ? new BigDecimal("100.00") : jdbc.queryForObject("SELECT ticket_price FROM match_ticket_zone WHERE match_id=?", BigDecimal.class, match);
            jdbc.update("INSERT INTO order_item(order_id,inventory_id,ticket_price,zone_name_snapshot,row_no_snapshot,seat_no_snapshot,item_status) VALUES(?,?,?,'IT14统计区','1排',?,?)", order, inventory, price, (first + n - 1) + "座", itemStatus);
            long item = id("SELECT item_id FROM order_item WHERE order_id=" + order + " ORDER BY item_id DESC LIMIT 1");
            String ticketStatus = itemStatus.equals("REFUNDED") ? "REFUNDED" : n <= usedCount ? "USED" : "UNUSED";
            jdbc.update("INSERT INTO e_ticket(ticket_code,order_id,item_id,ticket_status,issued_at,used_at) VALUES(?,?,?,?,'2026-08-20 12:30:00',CASE WHEN ?='USED' THEN '2026-09-10 20:00:00' ELSE NULL END)", PREFIX + "-T-" + suffix + "-" + n, order, item, ticketStatus, ticketStatus);
        }
    }
    private void payment(long order, String amount, String suffix, String time) {
        jdbc.update("INSERT INTO payment_record(payment_no,order_id,pay_amount,pay_method,pay_status,third_party_trade_no,pay_time,created_at) VALUES(?,?,?,'SIMULATED','SUCCESS',?,?,?)", PREFIX + "-P-" + suffix, order, new BigDecimal(amount), PREFIX + "-TRADE-" + suffix, time, time);
    }
    private void insertCheckins(long match, long checker, int count) {
        var tickets = jdbc.queryForList("SELECT t.ticket_id,t.ticket_code FROM e_ticket t JOIN ticket_order o ON o.order_id=t.order_id WHERE o.match_id=? AND t.ticket_status='USED' ORDER BY t.ticket_id LIMIT ?", match, count);
        for (var ticket : tickets) jdbc.update("INSERT INTO checkin_record(match_id,ticket_id,scanned_ticket_code,checker_id,check_result,check_time) VALUES(?,?,?,?, 'SUCCESS','2026-09-10 20:00:00')", match, ticket.get("ticket_id"), ticket.get("ticket_code"), checker);
    }
    private void addOtherStatusRefunds(long user, long admin) {
        long zone = id("SELECT z.match_zone_id FROM match_ticket_zone z JOIN match_info m ON m.match_id=z.match_id WHERE m.season_id<>" + seasonId + " ORDER BY z.match_zone_id LIMIT 1");
        long match = id("SELECT match_id FROM match_ticket_zone WHERE match_zone_id=" + zone);
        jdbc.update("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,paid_at,created_at) VALUES('IT14-O-RP',?,?,?,1,0,'REFUND_PENDING','2026-08-20 10:15:00','2026-08-20 10:00:00','2026-08-20 09:50:00')", user, match, zone);
        jdbc.update("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,paid_at,created_at) VALUES('IT14-O-RR',?,?,?,1,0,'PAID','2026-08-20 10:15:00','2026-08-20 10:00:00','2026-08-20 09:50:00')", user, match, zone);
        long pendingOrder = id("SELECT order_id FROM ticket_order WHERE order_no='IT14-O-RP'");
        long rejectedOrder = id("SELECT order_id FROM ticket_order WHERE order_no='IT14-O-RR'");
        payment(pendingOrder, "0.00", "RP", "2026-08-20 13:00:00");
        payment(rejectedOrder, "0.00", "RR", "2026-08-20 14:00:00");
        jdbc.update("INSERT INTO refund_apply(refund_no,order_id,applicant_id,reason,refund_amount,refund_status,created_at) VALUES('IT14-R-RP',?,?,'待审核统计',0,'PENDING','2026-08-20 16:00:00')", pendingOrder, user);
        jdbc.update("INSERT INTO refund_apply(refund_no,order_id,applicant_id,reason,refund_amount,refund_status,auditor_id,audit_remark,audit_time,created_at) VALUES('IT14-R-RR',?,?,'拒绝统计',0,'REJECTED',?,'拒绝','2026-08-20 17:00:00','2026-08-20 16:30:00')", rejectedOrder, user, admin);
    }
    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("username", username, "password", "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private String bearer(String token) { return "Bearer " + token; }
    private void cleanup() {
        jdbc.update("DELETE FROM checkin_record WHERE scanned_ticket_code LIKE 'IT14%'");
        jdbc.update("DELETE FROM refund_apply WHERE refund_no LIKE 'IT14%'");
        jdbc.update("DELETE FROM e_ticket WHERE ticket_code LIKE 'IT14%'");
        jdbc.update("DELETE FROM payment_record WHERE payment_no LIKE 'IT14%'");
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM ticket_order WHERE order_no LIKE 'IT14%')");
        jdbc.update("DELETE FROM match_seat_inventory WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT14统计赛季'))");
        jdbc.update("DELETE FROM ticket_order WHERE order_no LIKE 'IT14%'");
        jdbc.update("DELETE FROM match_ticket_zone WHERE match_id IN (SELECT match_id FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT14统计赛季'))");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT14统计赛季')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT14统计赛季')");
        jdbc.update("DELETE FROM season_info WHERE season_name='IT14统计赛季'");
        jdbc.update("DELETE FROM stadium_seat WHERE stadium_zone_id IN (SELECT stadium_zone_id FROM stadium_zone WHERE zone_code='IT14')");
        jdbc.update("DELETE FROM stadium_zone WHERE zone_code='IT14'");
    }
}
