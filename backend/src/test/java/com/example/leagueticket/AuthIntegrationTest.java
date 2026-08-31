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

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class AuthIntegrationTest {

    private static final String DEMO_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetAccounts() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'it\\_%'");
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        resetDemo("demo_user", "13800000001", "演示普通用户", hash);
        resetDemo("demo_admin", "13800000002", "演示管理员", hash);
        resetDemo("demo_club", "13800000003", "潮汐俱乐部管理员", hash);
        resetDemo("demo_event_admin", "13800000005", "演示赛事管理员", hash);
    }

    @Test
    void registrationValidationAndDuplicateChecks() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_register", "phone", "13900001001",
                                "password", "safe123", "roleCode", "USER", "realName", "集成注册用户"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("USER"))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_register_club", "phone", "13900001002",
                                "password", "safe123", "roleCode", "CLUB", "clubName", "集成注册俱乐部"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCode").value("CLUB"))
                .andExpect(jsonPath("$.data.realName").value("集成注册俱乐部"))
                .andExpect(jsonPath("$.data.clubId").doesNotExist())
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_register_event", "phone", "13900001003",
                                "password", "safe123", "roleCode", "EVENT_ADMIN", "employeeNo", "EA-IT-001"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCode").value("EVENT_ADMIN"))
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_register_admin", "phone", "13900001004",
                                "password", "safe123", "roleCode", "ADMIN", "employeeNo", "SA-IT-001"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"));

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username='it_register'", String.class);
        org.assertj.core.api.Assertions.assertThat(storedHash).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(storedHash).doesNotContain("safe123");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "demo_user", "phone", "13900001005",
                                "password", "safe123", "roleCode", "USER", "realName", "重复用户名"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_dup_phone", "phone", "13800000001",
                                "password", "safe123", "roleCode", "USER", "realName", "重复手机号"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_no_role", "phone", "13900001006", "password", "safe123"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_bad_role", "phone", "13900001007", "password", "safe123",
                                "roleCode", "CHECKER"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_club_no_name", "phone", "13900001008", "password", "safe123",
                                "roleCode", "CLUB"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_event_no_no", "phone", "13900001009", "password", "safe123",
                                "roleCode", "EVENT_ADMIN"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_admin_no_no", "phone", "13900001010", "password", "safe123",
                                "roleCode", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allRolesCanLoginAndInvalidAccountsAreRejected() throws Exception {
        assertRoleLogin("demo_user", "USER");
        assertRoleLogin("demo_club", "CLUB");
        assertRoleLogin("demo_event_admin", "EVENT_ADMIN");
        assertRoleLogin("demo_admin", "ADMIN");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "demo_user", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        jdbcTemplate.update("UPDATE sys_user SET user_status='DISABLED' WHERE username='demo_user'");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "demo_user", "password", DEMO_PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void jwtAuthenticationAndAuthorizationReturnCorrectStatusCodes() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());

        String userToken = loginToken("demo_user", DEMO_PASSWORD);
        String adminToken = loginToken("demo_admin", DEMO_PASSWORD);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray());
        mockMvc.perform(get("/api/admin/roles").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/roles").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
        mockMvc.perform(get("/api/admin/permissions").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].permissionCode").exists());
    }

    @Test
    void profileAndPasswordChangesAreProtected() throws Exception {
        String token = loginToken("demo_user", DEMO_PASSWORD);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("demo_user"));

        mockMvc.perform(put("/api/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("realName", "修改后的姓名", "phone", "13900002001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("修改后的姓名"));

        mockMvc.perform(put("/api/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("realName", "重复手机号", "phone", "13800000002"))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/users/me/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "wrong", "newPassword", "newpass123"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/users/me/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", DEMO_PASSWORD, "newPassword", "newpass123"))))
                .andExpect(status().isOk());
        loginToken("demo_user", "newpass123");
    }

    @Test
    void adminCanManageUsersAndUserCannot() throws Exception {
        String adminToken = loginToken("demo_admin", DEMO_PASSWORD);
        String userToken = loginToken("demo_user", DEMO_PASSWORD);
        Long clubId = jdbcTemplate.queryForObject("SELECT MIN(club_id) FROM club_info", Long.class);

        mockMvc.perform(post("/api/admin/users").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_club", "phone", "13900003001", "password", "safe123",
                                "realName", "集成俱乐部账号", "roleCode", "CLUB", "clubId", clubId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("CLUB"));

        mockMvc.perform(post("/api/admin/users").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_club_missing", "phone", "13900003002", "password", "safe123",
                                "realName", "缺俱乐部", "roleCode", "CLUB"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        Long createdId = jdbcTemplate.queryForObject("SELECT user_id FROM sys_user WHERE username='it_club'", Long.class);
        mockMvc.perform(get("/api/admin/users/{id}", createdId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users?roleCode=CLUB&page=1&size=10").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].roleCode").value("CLUB"));
        mockMvc.perform(put("/api/admin/users/{id}", createdId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("realName", "更新俱乐部账号", "phone", "13900003003",
                                "roleCode", "CLUB", "clubId", clubId, "userStatus", "ENABLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("更新俱乐部账号"));
        mockMvc.perform(put("/api/admin/users/{id}/status", createdId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("userStatus", "DISABLED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_club", "password", "safe123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registeredClubMustBeBoundBeforeEnableAndThenCanUseClubApis() throws Exception {
        String adminToken = loginToken("demo_admin", DEMO_PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_club_review", "phone", "13900003010",
                                "password", "safe123", "roleCode", "CLUB", "clubName", "待审核俱乐部"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"))
                .andExpect(jsonPath("$.data.clubId").doesNotExist());

        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM sys_user WHERE username='it_club_review'", Long.class);
        mockMvc.perform(put("/api/admin/users/{id}/status", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("userStatus", "ENABLED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CLUB账号启用前必须先绑定俱乐部"));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT user_status FROM sys_user WHERE user_id=?", String.class, userId)).isEqualTo("DISABLED");

        Long clubId = jdbcTemplate.queryForObject("SELECT MIN(club_id) FROM club_info", Long.class);
        mockMvc.perform(put("/api/admin/users/{id}", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("realName", "待审核俱乐部", "phone", "13900003010",
                                "roleCode", "CLUB", "clubId", clubId, "userStatus", "DISABLED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubId));
        mockMvc.perform(put("/api/admin/users/{id}/status", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("userStatus", "ENABLED"))))
                .andExpect(status().isOk());

        String clubToken = loginToken("it_club_review", "safe123");
        mockMvc.perform(get("/api/club/profile").header("Authorization", bearer(clubToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/club/players").header("Authorization", bearer(clubToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/club/coaches").header("Authorization", bearer(clubToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/matches").queryParam("clubId", String.valueOf(clubId))
                .header("Authorization", bearer(clubToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/club/statistics/overview").header("Authorization", bearer(clubToken))).andExpect(status().isOk());

        Map<String, Object> demoClub = jdbcTemplate.queryForMap("""
                SELECT r.role_code, u.user_status, u.club_id
                FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id
                WHERE u.username='demo_club'""");
        org.assertj.core.api.Assertions.assertThat(demoClub.get("role_code")).isEqualTo("CLUB");
        org.assertj.core.api.Assertions.assertThat(demoClub.get("user_status")).isEqualTo("ENABLED");
        org.assertj.core.api.Assertions.assertThat(demoClub.get("club_id")).isNotNull();
    }

    private void resetDemo(String username, String phone, String realName, String hash) {
        jdbcTemplate.update("UPDATE sys_user SET phone=?, display_name=?, password_hash=?, user_status='ENABLED' WHERE username=?",
                phone, realName, hash, username);
    }

    private void assertRoleLogin(String username, String roleCode) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value(roleCode))
                .andExpect(jsonPath("$.data.token", startsWith("eyJ")));
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
