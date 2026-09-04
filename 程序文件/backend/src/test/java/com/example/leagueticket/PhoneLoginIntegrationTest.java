package com.example.leagueticket;

import com.example.leagueticket.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
class PhoneLoginIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM sys_user WHERE phone LIKE '1391711%'");
        String hash = encoder.encode("123456");
        jdbc.update("UPDATE sys_user SET username='demo_user',phone='13800000001',password_hash=?,user_status='ENABLED' WHERE user_id=1", hash);
        jdbc.update("UPDATE sys_user SET username='demo_club',phone='13800000003',password_hash=?,user_status='ENABLED' WHERE user_id=3", hash);
        jdbc.update("UPDATE sys_user SET username='demo_event_admin',phone='13800000005',password_hash=?,user_status='ENABLED' WHERE user_id=4", hash);
        jdbc.update("UPDATE sys_user SET username='demo_admin',phone='13800000002',password_hash=?,user_status='ENABLED' WHERE user_id=2", hash);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM sys_user WHERE phone LIKE '1391711%' OR username='历史空手机号'");
    }

    @Test
    void allFourRolesUsePhoneAndLegacyUsernameLoginIsRejected() throws Exception {
        role("13800000001", "USER");
        role("13800000003", "CLUB");
        role("13800000005", "EVENT_ADMIN");
        role("13800000002", "ADMIN");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forRole("13917119999", "123456", "USER", null))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forPhone("13800000001", "wrong-password"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "demo_user", "password", "123456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateNicknameAccountsLoginToDifferentStableUserIds() throws Exception {
        registerUser("柚子", "13917110001", "甲").andExpect(status().isOk());
        registerUser("柚子", "13917110002", "乙").andExpect(status().isOk());
        JsonNode loginA = login("13917110001", "safe123");
        JsonNode loginB = login("13917110002", "safe123");
        long userA = loginA.path("data").path("userId").asLong();
        long userB = loginB.path("data").path("userId").asLong();
        assertThat(userA).isNotEqualTo(userB);
        assertThat(jwtService.parseToken(loginA.path("data").path("token").asText()).getSubject()).isEqualTo(String.valueOf(userA));
        assertThat(jwtService.parseToken(loginB.path("data").path("token").asText()).getSubject()).isEqualTo(String.valueOf(userB));
        me(loginA).andExpect(jsonPath("$.data.userId").value(userA)).andExpect(jsonPath("$.data.username").value("柚子"));
        me(loginB).andExpect(jsonPath("$.data.userId").value(userB)).andExpect(jsonPath("$.data.username").value("柚子"));
    }

    @Test
    void phoneRemainsUniqueAndDisabledAccountCannotLogin() throws Exception {
        registerUser("唯一手机号", "13917110003", "甲").andExpect(status().isOk());
        registerUser("另一个昵称", "13917110003", "乙").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE phone='13917110003'", Integer.class)).isEqualTo(1);
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,employee_no,role_id,user_status) " +
                        "SELECT '待启用赛事','13917110004',?,'待启用','EA1701',role_id,'DISABLED' FROM sys_role WHERE role_code='EVENT_ADMIN'",
                encoder.encode("safe123"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forRole("13917110004", "safe123", "EVENT_ADMIN", "EA1701"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void nicknameAndPhoneChangeKeepExistingJwtBoundToUserId() throws Exception {
        registerUser("旧昵称", "13917110005", "甲").andExpect(status().isOk());
        registerUser("共享昵称", "13917110006", "乙").andExpect(status().isOk());
        JsonNode login = login("13917110005", "safe123");
        String token = login.path("data").path("token").asText();
        long userId = login.path("data").path("userId").asLong();
        mvc.perform(put("/api/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "username", "共享昵称", "phone", "13917110007", "realName", "甲更新"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.username").value("共享昵称"));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.username").value("共享昵称"))
                .andExpect(jsonPath("$.data.phone").value("13917110007"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forRole("13917110005", "safe123", "USER", null))))
                .andExpect(status().isUnauthorized());
        assertThat(login("13917110007", "safe123").path("data").path("userId").asLong()).isEqualTo(userId);
    }

    @Test
    void historicalAccountWithoutPhoneCannotBeEnabled() throws Exception {
        String hash = encoder.encode("safe123");
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,role_id,user_status) " +
                "SELECT '历史空手机号',NULL,?,'历史账号',role_id,'DISABLED' FROM sys_role WHERE role_code='USER'", hash);
        long id = jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='历史空手机号' ORDER BY user_id DESC LIMIT 1", Long.class);
        String adminToken = login("13800000002", "123456").path("data").path("token").asText();
        mvc.perform(put("/api/admin/users/{id}/status", id).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "ENABLED"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("账号启用前必须设置有效手机号"));
        jdbc.update("DELETE FROM sys_user WHERE user_id=?", id);
    }

    private org.springframework.test.web.servlet.ResultActions registerUser(String username, String phone, String name) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                "username", username, "phone", phone, "password", "safe123", "realName", name, "roleCode", "USER"))));
    }

    private void role(String phone, String roleCode) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forPhone(phone, "123456"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCode").value(roleCode));
    }

    private JsonNode login(String phone, String password) throws Exception {
        String content = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forPhone(phone, password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(content);
    }

    private org.springframework.test.web.servlet.ResultActions me(JsonNode login) throws Exception {
        return mvc.perform(get("/api/auth/me").header("Authorization", bearer(login.path("data").path("token").asText())))
                .andExpect(status().isOk());
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private static String bearer(String token) { return "Bearer " + token; }
}
