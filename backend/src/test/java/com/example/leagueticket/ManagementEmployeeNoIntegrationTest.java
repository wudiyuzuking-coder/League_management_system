package com.example.leagueticket;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ManagementEmployeeNoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    String adminToken;

    @BeforeEach
    void setup() throws Exception {
        jdbc.update("DELETE FROM sys_user WHERE username LIKE 'p17a\\_%' AND phone <> '13917000051'");
        jdbc.update("DELETE FROM club_info WHERE club_name='P17A独立测试俱乐部'");
        String hash = encoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED',employee_no='SA0001' WHERE username='demo_admin'", hash);
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED',employee_no='EA0001' WHERE username='demo_event_admin'", hash);
        adminToken = loginByPhone("13800000002", "123456");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM sys_user WHERE username LIKE 'p17a\\_%' AND phone <> '13917000051'");
        jdbc.update("DELETE FROM club_info WHERE club_name='P17A独立测试俱乐部'");
    }

    @Test
    void publicManagementApplicationsAreRejectedAndAdminCreationStoresFieldsSeparately() throws Exception {
        registerManagement("p17a_event_apply", "13917000001", "张三", "EA0101", "EVENT_ADMIN")
                .andExpect(status().isForbidden());
        registerManagement("p17a_admin_apply", "13917000002", "李四", "SA0101", "ADMIN")
                .andExpect(status().isForbidden());

        adminCreate("p17a_event_apply", "13917000001", "张三", "EA0101", "EVENT_ADMIN")
                .andExpect(status().isOk());
        Map<String, Object> stored = jdbc.queryForMap("SELECT display_name,employee_no FROM sys_user WHERE username='p17a_event_apply'");
        assertThat(stored.get("display_name")).isEqualTo("张三");
        assertThat(stored.get("employee_no")).isEqualTo("EA0101");
    }

    @Test
    void invalidFormatsRolePrefixesAndNonManagementEmployeeNumbersAreRejected() throws Exception {
        List<Map<String, String>> invalid = List.of(
                management("p17a_bad_1", "13917000011", "EVENT_ADMIN", "SA0001"),
                management("p17a_bad_2", "13917000012", "EVENT_ADMIN", "EA001"),
                management("p17a_bad_3", "13917000013", "EVENT_ADMIN", "ea0001"),
                management("p17a_bad_4", "13917000014", "ADMIN", "EA0001"),
                management("p17a_bad_5", "13917000015", "ADMIN", "SA001"));
        for (Map<String, String> request : invalid) {
            mvc.perform(post("/api/admin/users").header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "p17a_user_no", "phone", "13917000016", "password", "safe123",
                                "roleCode", "USER", "realName", "普通用户", "employeeNo", "EA0999"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("当前角色不允许设置管理人员工号"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username LIKE 'p17a_bad_%' OR username='p17a_user_no'", Integer.class)).isZero();
    }

    @Test
    void duplicateEmployeeNumberReturnsConflictAndDatabaseKeepsOneRow() throws Exception {
        adminCreate("p17a_dup_1", "13917000021", "甲", "EA0201", "EVENT_ADMIN").andExpect(status().isOk());
        adminCreate("p17a_dup_2", "13917000022", "乙", "EA0201", "EVENT_ADMIN")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("管理人员工号已存在"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE employee_no='EA0201'", Integer.class)).isEqualTo(1);
    }

    @Test
    void adminCreateEditListAndRoleChangeUseTheSameRules() throws Exception {
        JsonNode created = response(mvc.perform(post("/api/admin/users").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "username", "p17a_created_admin", "phone", "13917000031", "password", "safe123",
                                "realName", "后台管理员", "employeeNo", "SA0301", "roleCode", "ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.employeeNo").value("SA0301")));
        long userId = created.path("data").path("userId").asLong();

        mvc.perform(get("/api/admin/users/{id}", userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.realName").value("后台管理员"))
                .andExpect(jsonPath("$.data.employeeNo").value("SA0301"));
        mvc.perform(get("/api/admin/users?roleCode=ADMIN&size=100").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[*].employeeNo", hasItem("SA0301")));

        update(userId, "更名管理员", "ADMIN", "SA0302", "ENABLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("SA0302"));
        update(userId, "错误切换", "EVENT_ADMIN", "SA0302", "ENABLED").andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT employee_no FROM sys_user WHERE user_id=?", String.class, userId)).isEqualTo("SA0302");
        update(userId, "赛事管理员", "EVENT_ADMIN", "EA0302", "ENABLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("EVENT_ADMIN"));
    }

    @Test
    void historicalManagementAccountsNeedEmployeeNoBeforeEnable() throws Exception {
        String hash = encoder.encode("safe123");
        long eventId = insertHistorical("p17a_history_event", "13917000041", "历史赛事管理员", "EVENT_ADMIN", hash);
        long adminId = insertHistorical("p17a_history_admin", "13917000042", "历史系统管理员", "ADMIN", hash);
        for (long id : List.of(eventId, adminId)) {
            mvc.perform(put("/api/admin/users/{id}/status", id).header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "ENABLED"))))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("管理账号启用前必须设置合法工号"));
        }
        update(eventId, "历史赛事管理员", "EVENT_ADMIN", "EA0401", "DISABLED").andExpect(status().isOk());
        update(adminId, "历史系统管理员", "ADMIN", "SA0401", "DISABLED").andExpect(status().isOk());
        enable(eventId).andExpect(status().isOk());
        enable(adminId).andExpect(status().isOk());
    }

    @Test
    void clubRegistrationBindingAndEnableFlowStillWorks() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "username", "p17a_it_club", "phone", "13917000052", "password", "safe123",
                        "roleCode", "CLUB", "realName", "阶段17A负责人", "clubName", "阶段17A俱乐部"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userStatus").value("DISABLED"))
                .andExpect(jsonPath("$.data.employeeNo").doesNotExist());
        long userId = jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username='p17a_it_club'", Long.class);
        enable(userId).andExpect(status().isConflict());
        jdbc.update("INSERT INTO club_info(club_name,home_city,club_status) VALUES('P17A独立测试俱乐部','测试城','ACTIVE')");
        long clubId = jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name='P17A独立测试俱乐部'", Long.class);
        mvc.perform(put("/api/admin/users/{id}", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "username", "p17a_it_club", "realName", "阶段17A俱乐部", "phone", "13917000052", "roleCode", "CLUB",
                                "clubId", clubId, "userStatus", "DISABLED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubId));
        enable(userId).andExpect(status().isOk());
        loginByPhone("13917000052", "safe123");
    }

    private org.springframework.test.web.servlet.ResultActions registerManagement(String username, String phone,
                                                                                   String realName, String employeeNo,
                                                                                   String roleCode) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "phone", phone, "password", "safe123",
                        "realName", realName, "employeeNo", employeeNo, "roleCode", roleCode))));
    }

    private org.springframework.test.web.servlet.ResultActions adminCreate(String username, String phone,
                                                                            String realName, String employeeNo,
                                                                            String roleCode) throws Exception {
        return mvc.perform(post("/api/admin/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "phone", phone, "password", "safe123",
                        "realName", realName, "employeeNo", employeeNo, "roleCode", roleCode))));
    }

    private Map<String, String> management(String username, String phone, String roleCode, String employeeNo) {
        return Map.of("username", username, "phone", phone, "password", "safe123", "realName", "格式测试",
                "employeeNo", employeeNo, "roleCode", roleCode);
    }

    private org.springframework.test.web.servlet.ResultActions update(long id, String name, String role,
                                                                       String employeeNo, String status) throws Exception {
        return mvc.perform(put("/api/admin/users/{id}", id).header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "username", jdbc.queryForObject("SELECT username FROM sys_user WHERE user_id=?", String.class, id),
                        "realName", name,
                        "phone", jdbc.queryForObject("SELECT phone FROM sys_user WHERE user_id=?", String.class, id),
                        "roleCode", role, "employeeNo", employeeNo, "userStatus", status))));
    }

    private org.springframework.test.web.servlet.ResultActions enable(long id) throws Exception {
        return mvc.perform(put("/api/admin/users/{id}/status", id).header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "ENABLED"))));
    }

    private long insertHistorical(String username, String phone, String name, String roleCode, String hash) {
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,employee_no,role_id,club_id,user_status) " +
                        "SELECT ?,?,?,?,NULL,role_id,NULL,'DISABLED' FROM sys_role WHERE role_code=?",
                username, phone, hash, name, roleCode);
        return jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username=?", Long.class, username);
    }

    private String loginByPhone(String phone, String password) throws Exception {
        String content = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestLoginPayload.forPhone(phone, password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(content).path("data").path("token").asText();
    }

    private JsonNode response(org.springframework.test.web.servlet.ResultActions action) throws Exception {
        return json.readTree(action.andReturn().getResponse().getContentAsString());
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private static String bearer(String token) { return "Bearer " + token; }
}
