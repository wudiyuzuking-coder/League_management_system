package com.example.leagueticket;

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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ClubManagementIntegrationTest {
    private static final String PASSWORD = "123456";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetStageFourData() {
        jdbcTemplate.update("DELETE FROM player_season_stat WHERE appearances IN (7,44)");
        jdbcTemplate.update("DELETE FROM player_info WHERE player_name LIKE 'IT4%'");
        jdbcTemplate.update("DELETE FROM coach_info WHERE coach_name LIKE 'IT4%'");
        jdbcTemplate.update("DELETE FROM club_info WHERE club_name LIKE 'IT4%'");
        Long clubA = clubIdFor("demo_club");
        jdbcTemplate.update("UPDATE club_info SET club_name='杭州潮汐足球俱乐部',short_name='杭州潮汐',home_city='杭州',description='演示俱乐部A',club_status='ACTIVE' WHERE club_id=?", clubA);
        String hash = passwordEncoder.encode(PASSWORD);
        jdbcTemplate.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE username IN ('demo_user','demo_admin','demo_club','demo_checker')", hash);
    }

    @Test
    void clubAdministrationAndClubSelfScopeWork() throws Exception {
        String admin = loginByPhone("13800000002");
        String club = loginByPhone("13800000003");
        String user = loginByPhone("13800000001");

        mockMvc.perform(post("/api/admin/clubs").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(clubBody("IT4新俱乐部", "新城"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubStatus").value("ACTIVE"));
        Long createdId = jdbcTemplate.queryForObject("SELECT club_id FROM club_info WHERE club_name='IT4新俱乐部'", Long.class);
        mockMvc.perform(put("/api/admin/clubs/{id}", createdId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(clubBody("IT4更新俱乐部", "更新城"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.homeCity").value("更新城"));
        mockMvc.perform(put("/api/admin/clubs/{id}/status", createdId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clubStatus\":\"DISABLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/club/profile").header("Authorization", bearer(club)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubIdFor("demo_club")));
        mockMvc.perform(put("/api/club/profile").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(clubBody("杭州潮汐足球俱乐部", "杭州"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubName").value("杭州潮汐足球俱乐部"));
        mockMvc.perform(get("/api/admin/clubs/{id}", createdId).header("Authorization", bearer(club)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/club/profile").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clubs?name=潮汐&page=1&size=10").header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void playerRulesAndCrossClubScopeAreEnforced() throws Exception {
        String club = loginByPhone("13800000003");
        String admin = loginByPhone("13800000002");
        Long clubA = clubIdFor("demo_club");
        Long clubB = otherClubId(clubA);

        mockMvc.perform(post("/api/club/players").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(playerBody("IT4本队球员", 88, "FORWARD"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubA));
        mockMvc.perform(post("/api/club/players").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(playerBody("IT4重复号码", 88, "DEFENDER"))))
                .andExpect(status().isConflict());
        Long ownPlayer = jdbcTemplate.queryForObject("SELECT player_id FROM player_info WHERE player_name='IT4本队球员'", Long.class);
        mockMvc.perform(put("/api/club/players/{id}", ownPlayer).header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(playerBody("IT4更新球员", 87, "MIDFIELDER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shirtNo").value(87));

        Long otherPlayer = jdbcTemplate.queryForObject("SELECT player_id FROM player_info WHERE club_id=? ORDER BY player_id LIMIT 1", Long.class, clubB);
        mockMvc.perform(put("/api/club/players/{id}", otherPlayer).header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(playerBody("越权修改", 66, "FORWARD"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/clubs/{clubId}/players", clubB).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(playerBody("IT4管理员球员", 77, "DEFENDER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubB));
        Long adminPlayer = jdbcTemplate.queryForObject("SELECT player_id FROM player_info WHERE player_name='IT4管理员球员'", Long.class);
        mockMvc.perform(put("/api/admin/players/{id}/status", adminPlayer).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"playerStatus\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void coachRulesAndCrossClubScopeAreEnforced() throws Exception {
        String club = loginByPhone("13800000003");
        String admin = loginByPhone("13800000002");
        Long clubA = clubIdFor("demo_club");
        Long clubB = otherClubId(clubA);

        mockMvc.perform(post("/api/club/coaches").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(coachBody("IT4本队教练", "ASSISTANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubA));
        Long ownCoach = jdbcTemplate.queryForObject("SELECT coach_id FROM coach_info WHERE coach_name='IT4本队教练'", Long.class);
        mockMvc.perform(put("/api/club/coaches/{id}", ownCoach).header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(coachBody("IT4更新教练", "HEAD_COACH"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("HEAD_COACH"));

        mockMvc.perform(post("/api/admin/clubs/{clubId}/coaches", clubB).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(coachBody("IT4其他教练", "HEAD_COACH"))))
                .andExpect(status().isOk());
        Long otherCoach = jdbcTemplate.queryForObject("SELECT coach_id FROM coach_info WHERE coach_name='IT4其他教练'", Long.class);
        mockMvc.perform(put("/api/club/coaches/{id}", otherCoach).header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(coachBody("越权教练", "HEAD_COACH"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/coaches/{id}", otherCoach).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(coachBody("IT4管理员更新教练", "ASSISTANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubB));
    }

    @Test
    void playerSeasonStatRulesAndAdminScopeWork() throws Exception {
        String club = loginByPhone("13800000003");
        String admin = loginByPhone("13800000002");
        Long clubA = clubIdFor("demo_club");
        Long clubB = otherClubId(clubA);
        Long seasonId = jdbcTemplate.queryForObject("SELECT MIN(season_id) FROM season_info", Long.class);
        jdbcTemplate.update("INSERT INTO player_info(club_id,player_name,shirt_no,position,player_status) VALUES(?,?,?,?,?)",
                clubA, "IT4统计球员", 86, "FORWARD", "ACTIVE");
        Long ownPlayer = jdbcTemplate.queryForObject("SELECT player_id FROM player_info WHERE player_name='IT4统计球员'", Long.class);
        Long otherPlayer = jdbcTemplate.queryForObject("SELECT player_id FROM player_info WHERE club_id=? ORDER BY player_id LIMIT 1", Long.class, clubB);

        String body = json(statBody(seasonId, ownPlayer, 7, 3, 2));
        mockMvc.perform(post("/api/club/player-season-stats").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.goals").value(3));
        mockMvc.perform(post("/api/club/player-season-stats").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/club/player-season-stats").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(statBody(seasonId, otherPlayer, 7, 1, 1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/club/player-season-stats").header("Authorization", bearer(club))
                        .contentType(MediaType.APPLICATION_JSON).content(json(statBody(seasonId, ownPlayer, -1, 0, 0))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/clubs/{clubId}/player-season-stats", clubB).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(statBody(seasonId, otherPlayer, 44, 2, 1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubB));
    }

    private Map<String, Object> clubBody(String name, String city) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clubName", name); map.put("shortName", name); map.put("homeCity", city); map.put("description", "阶段4集成测试");
        return map;
    }

    private Map<String, Object> playerBody(String name, int shirt, String position) {
        return Map.of("playerName", name, "shirtNo", shirt, "position", position, "nationality", "中国");
    }

    private Map<String, Object> coachBody(String name, String title) {
        return Map.of("coachName", name, "title", title, "nationality", "中国", "description", "阶段4集成测试");
    }

    private Map<String, Object> statBody(Long seasonId, Long playerId, int appearances, int goals, int assists) {
        return Map.of("seasonId", seasonId, "playerId", playerId, "appearances", appearances, "goals", goals, "assists", assists);
    }

    private Long clubIdFor(String username) {
        return jdbcTemplate.queryForObject("SELECT club_id FROM sys_user WHERE username=?", Long.class, username);
    }

    private Long otherClubId(Long clubId) {
        return jdbcTemplate.queryForObject("SELECT club_id FROM club_info WHERE club_id != ? ORDER BY club_id LIMIT 1", Long.class, clubId);
    }

    private String loginByPhone(String phone) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(TestLoginPayload.forPhone(phone, PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
}
