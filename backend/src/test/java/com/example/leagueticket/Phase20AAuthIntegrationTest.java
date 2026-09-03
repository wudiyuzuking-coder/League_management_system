package com.example.leagueticket;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase20AAuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM sys_user WHERE phone LIKE '1392000%'");
        String hash = encoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE phone IN ('13800000001','13800000002','13800000003','13800000005')", hash);
        jdbc.update("UPDATE sys_user SET employee_no='SA0001' WHERE phone='13800000002'");
        jdbc.update("UPDATE sys_user SET employee_no='EA0001' WHERE phone='13800000005'");
    }

    @Test
    void allFourRolesLoginWithTheirFormalIdentityFields() throws Exception {
        login(TestLoginPayload.forPhone("13800000001", "123456")).andExpect(status().isOk());
        login(TestLoginPayload.forPhone("13800000003", "123456")).andExpect(status().isOk());
        login(TestLoginPayload.forPhone("13800000005", "123456")).andExpect(status().isOk());
        login(TestLoginPayload.forPhone("13800000002", "123456")).andExpect(status().isOk());
    }

    @Test
    void phonePasswordAndRoleErrorsAreDistinguished() throws Exception {
        login(TestLoginPayload.forRole("13920009999", "123456", "USER", null))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("手机号不存在"));
        login(TestLoginPayload.forRole("13800000001", "wrong-password", "USER", null))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("密码错误"));
        login(TestLoginPayload.forRole("13800000001", "123456", "CLUB", null))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("所选身份与账号不匹配"));
    }

    @Test
    void managementEmployeeNumberIsRequiredAndMustMatch() throws Exception {
        login(TestLoginPayload.forRole("13800000005", "123456", "EVENT_ADMIN", null))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请输入工号"));
        login(TestLoginPayload.forRole("13800000005", "123456", "EVENT_ADMIN", "EA9999"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("工号与账号不匹配"));
        login(TestLoginPayload.forRole("13800000002", "123456", "ADMIN", "SA9999"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("工号与账号不匹配"));
    }

    @Test
    void disabledAccountIsRejectedAfterIdentityMatches() throws Exception {
        jdbc.update("UPDATE sys_user SET user_status='DISABLED' WHERE phone='13800000001'");
        login(TestLoginPayload.forPhone("13800000001", "123456"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("账号尚未启用"));
    }

    @Test
    void managementRolesCannotUsePublicRegistration() throws Exception {
        register(Map.of("username", "阶段20赛事", "phone", "13920000001", "password", "safe123",
                        "realName", "张三", "employeeNo", "EA2001", "roleCode", "EVENT_ADMIN"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("当前身份不支持公开注册，请联系系统管理员创建账号"));
        register(Map.of("username", "阶段20系统", "phone", "13920000002", "password", "safe123",
                        "realName", "李四", "employeeNo", "SA2001", "roleCode", "ADMIN"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("当前身份不支持公开注册，请联系系统管理员创建账号"));
    }

    @Test
    void userRegistrationRequiresRealNameAndStoresNicknameSeparately() throws Exception {
        register(Map.of("username", "阶段20昵称", "phone", "13920000003", "password", "safe123", "roleCode", "USER"))
                .andExpect(status().isBadRequest());
        register(Map.of("username", "阶段20昵称", "phone", "13920000003", "password", "safe123",
                        "realName", "真实姓名", "roleCode", "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("阶段20昵称"))
                .andExpect(jsonPath("$.data.realName").value("真实姓名"));
    }

    private org.springframework.test.web.servlet.ResultActions login(Map<String, String> payload) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)));
    }

    private org.springframework.test.web.servlet.ResultActions register(Map<String, String> payload) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)));
    }
}
