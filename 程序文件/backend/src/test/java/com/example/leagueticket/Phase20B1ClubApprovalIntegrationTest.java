package com.example.leagueticket;

import com.example.leagueticket.dto.ClubApprovalRequest;
import com.example.leagueticket.service.SysUserService;
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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class Phase20B1ClubApprovalIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SysUserService userService;

    String adminToken;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        String hash = encoder.encode("123456");
        jdbc.update("UPDATE sys_user SET password_hash=?,user_status='ENABLED' WHERE phone IN ('13800000001','13800000002','13800000003','13800000005')", hash);
        adminToken = login("13800000002", "123456");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM sys_user WHERE phone LIKE '139210%'");
        jdbc.update("DELETE FROM club_info WHERE club_name LIKE 'P20B1%'");
    }

    @Test
    void clubRegistrationSeparatesLeaderAndApplicationName() throws Exception {
        register("13921000001", "申请昵称", "张三", "P20B1银河FC")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("张三"))
                .andExpect(jsonPath("$.data.clubApplyName").value("P20B1银河FC"))
                .andExpect(jsonPath("$.data.clubId").doesNotExist())
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"));
        Map<String, Object> stored = jdbc.queryForMap(
                "SELECT display_name,club_apply_name,club_id,user_status FROM sys_user WHERE phone='13921000001'");
        assertThat(stored.get("display_name")).isEqualTo("张三");
        assertThat(stored.get("club_apply_name")).isEqualTo("P20B1银河FC");
        assertThat(stored.get("club_id")).isNull();

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "缺姓名", "phone", "13921000002", "password", "safe123",
                                "roleCode", "CLUB", "clubName", "P20B1缺姓名"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "缺名称", "phone", "13921000003", "password", "safe123",
                                "roleCode", "CLUB", "realName", "负责人"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createNewCreatesMinimalClubAndProfileCanCompleteCity() throws Exception {
        long userId = registeredUser("13921000011", "银河昵称", "张三", "P20B1银河足球俱乐部");
        approve(userId, Map.of("mode", "CREATE_NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT u.display_name,u.club_apply_name,u.club_id,u.user_status,c.club_name,c.home_city
                FROM sys_user u JOIN club_info c ON c.club_id=u.club_id WHERE u.user_id=?
                """, userId);
        assertThat(row.get("display_name")).isEqualTo("张三");
        assertThat(row.get("club_apply_name")).isEqualTo("P20B1银河足球俱乐部");
        assertThat(row.get("club_name")).isEqualTo("P20B1银河足球俱乐部");
        assertThat(row.get("home_city")).isNull();
        assertThat(row.get("user_status")).isEqualTo("ENABLED");

        String token = login("13921000011", "safe123");
        mvc.perform(get("/api/club/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeCity").doesNotExist())
                .andExpect(jsonPath("$.data.leaderName").value("张三"));
        mvc.perform(put("/api/club/profile").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("clubName", "P20B1银河足球俱乐部", "homeCity", "新加坡"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.homeCity").value("新加坡"));
    }

    @Test
    void bindExistingDoesNotCreateClubAndReturnsLeaderDetails() throws Exception {
        long clubId = createClub("P20B1待关联俱乐部", null);
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM club_info", Long.class);
        long userId = registeredUser("13921000021", "关联昵称", "李四", "P20B1申请名称");
        approve(userId, Map.of("mode", "BIND_EXISTING", "existingClubId", clubId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.clubId").value(clubId));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_info", Long.class)).isEqualTo(before);

        mvc.perform(get("/api/admin/clubs").header("Authorization", bearer(adminToken))
                        .queryParam("name", "P20B1待关联俱乐部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].leaderName").value("李四"))
                .andExpect(jsonPath("$.data.records[0].leaderPhone").value("13921000021"))
                .andExpect(jsonPath("$.data.records[0].leaderStatus").value("ENABLED"));
        mvc.perform(get("/api/admin/clubs/{id}", clubId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaderNickname").value("关联昵称"));
    }

    @Test
    void existingLeaderConflictKeepsSecondApplicationUntouched() throws Exception {
        long clubId = createClub("P20B1唯一负责人俱乐部", "测试城");
        long first = registeredUser("13921000031", "第一负责人", "甲", "P20B1甲");
        approve(first, Map.of("mode", "BIND_EXISTING", "existingClubId", clubId)).andExpect(status().isOk());
        long second = registeredUser("13921000032", "第二负责人", "乙", "P20B1乙");
        approve(second, Map.of("mode", "BIND_EXISTING", "existingClubId", clubId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该俱乐部已有负责人"));
        Map<String, Object> secondRow = jdbc.queryForMap("SELECT club_id,user_status FROM sys_user WHERE user_id=?", second);
        assertThat(secondRow.get("club_id")).isNull();
        assertThat(secondRow.get("user_status")).isEqualTo("DISABLED");
    }

    @Test
    void generalEnableStillRejectsUnreviewedClub() throws Exception {
        long userId = registeredUser("13921000041", "未审核", "王五", "P20B1未审核俱乐部");
        mvc.perform(put("/api/admin/users/{id}/status", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "ENABLED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CLUB账号启用前必须先绑定俱乐部"));
    }

    @Test
    void approvalIsAdminOnly() throws Exception {
        long userId = registeredUser("13921000051", "权限申请", "赵六", "P20B1权限俱乐部");
        for (String phone : new String[]{"13800000001", "13800000003", "13800000005"}) {
            String token = login(phone, "123456");
            mvc.perform(post("/api/admin/users/{id}/club-approval", userId)
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mode", "CREATE_NEW"))))
                    .andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("SELECT club_id FROM sys_user WHERE user_id=?", Long.class, userId)).isNull();
    }

    @Test
    void disabledLeaderRemainsVisibleAndCanBeReenabled() throws Exception {
        long userId = registeredUser("13921000061", "停用昵称", "孙七", "P20B1停用展示");
        approve(userId, Map.of("mode", "CREATE_NEW")).andExpect(status().isOk());
        long clubId = jdbc.queryForObject("SELECT club_id FROM sys_user WHERE user_id=?", Long.class, userId);
        mvc.perform(put("/api/admin/users/{id}/status", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "DISABLED"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/clubs/{id}", clubId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaderName").value("孙七"))
                .andExpect(jsonPath("$.data.leaderStatus").value("DISABLED"));
        mvc.perform(put("/api/admin/users/{id}/status", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("userStatus", "ENABLED"))))
                .andExpect(status().isOk());
    }

    @Test
    void concurrentBindingAllowsOnlyOneLeader() throws Exception {
        long clubId = createClub("P20B1并发绑定俱乐部", null);
        long first = registeredUser("13921000071", "并发甲", "并发甲", "P20B1并发甲");
        long second = registeredUser("13921000072", "并发乙", "并发乙", "P20B1并发乙");
        int successes = concurrentApprovals(
                () -> userService.approveClub(first, new ClubApprovalRequest("BIND_EXISTING", clubId)),
                () -> userService.approveClub(second, new ClubApprovalRequest("BIND_EXISTING", clubId)));
        assertThat(successes).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE club_id=?", Integer.class, clubId)).isEqualTo(1);
    }

    @Test
    void concurrentCreateNewCreatesNoOrphanClub() throws Exception {
        long userId = registeredUser("13921000081", "并发审核", "并发负责人", "P20B1并发新俱乐部");
        int successes = concurrentApprovals(
                () -> userService.approveClub(userId, new ClubApprovalRequest("CREATE_NEW", null)),
                () -> userService.approveClub(userId, new ClubApprovalRequest("CREATE_NEW", null)));
        assertThat(successes).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_info WHERE club_name='P20B1并发新俱乐部'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT club_id FROM sys_user WHERE user_id=?", Long.class, userId)).isNotNull();
    }

    @Test
    void adminManualClubCreationStillRequiresHomeCity() throws Exception {
        mvc.perform(post("/api/admin/clubs").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("clubName", "P20B1手工缺城市"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/clubs").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("clubName", "P20B1手工完整俱乐部", "homeCity", "上海"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.homeCity").value("上海"));
    }

    private int concurrentApprovals(Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> runApproval(first, ready, start));
            Future<Boolean> b = pool.submit(() -> runApproval(second, ready, start));
            ready.await();
            start.countDown();
            return (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean runApproval(Runnable action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            action.run();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private org.springframework.test.web.servlet.ResultActions register(String phone, String nickname,
                                                                         String realName, String clubName) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", nickname, "phone", phone, "password", "safe123",
                        "roleCode", "CLUB", "realName", realName, "clubName", clubName))));
    }

    private long registeredUser(String phone, String nickname, String realName, String clubName) throws Exception {
        register(phone, nickname, realName, clubName).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT user_id FROM sys_user WHERE phone=?", Long.class, phone);
    }

    private org.springframework.test.web.servlet.ResultActions approve(long userId, Map<String, Object> request) throws Exception {
        return mvc.perform(post("/api/admin/users/{id}/club-approval", userId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body(request)));
    }

    private long createClub(String name, String city) {
        jdbc.update("INSERT INTO club_info(club_name,home_city,club_status) VALUES(?,?,'ACTIVE')", name, city);
        return jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name=?", Long.class, name);
    }

    private String login(String phone, String password) throws Exception {
        Map<String, String> payload = phone.startsWith("139210")
                ? TestLoginPayload.forRole(phone, password, "CLUB", null)
                : TestLoginPayload.forPhone(phone, password);
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(payload)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(response);
        return root.path("data").path("token").asText();
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private static String bearer(String token) { return "Bearer " + token; }
}
