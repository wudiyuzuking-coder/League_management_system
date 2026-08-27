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
        resetDemo("demo_checker", "13800000004", "潮汐检票员", hash);
    }

    @Test
    void registrationValidationAndDuplicateChecks() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_register", "phone", "13900001001",
                                "password", "safe123", "realName", "集成注册用户"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("USER"));

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username='it_register'", String.class);
        org.assertj.core.api.Assertions.assertThat(storedHash).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(storedHash).doesNotContain("safe123");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "demo_user", "phone", "13900001002",
                                "password", "safe123", "realName", "重复用户名"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "it_dup_phone", "phone", "13800000001",
                                "password", "safe123", "realName", "重复手机号"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allRolesCanLoginAndInvalidAccountsAreRejected() throws Exception {
        assertRoleLogin("demo_user", "USER");
        assertRoleLogin("demo_club", "CLUB");
        assertRoleLogin("demo_checker", "CHECKER");
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
