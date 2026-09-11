package com.example.leagueticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase20B1 regression rewritten for the Phase25 create-new-only approval model. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase20B1ClubApprovalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registrationIsPendingAndCreateNewApprovalIsAtomic() throws Exception {
        String phone = "13921000011";
        register(phone, "银河昵称", "张三", "P20B1银河足球俱乐部")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("PENDING_CLUB_APPROVAL"));
        long userId = jdbc.queryForObject("SELECT user_id FROM sys_user WHERE phone=? AND role_id=(SELECT role_id FROM sys_role WHERE role_code='CLUB')", Long.class, phone);
        String admin = adminToken();
        mvc.perform(post("/api/admin/club-applications/{id}/approve", userId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CREATE_NEW\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userStatus").value("ENABLED"));
        Map<String,Object> row=jdbc.queryForMap("SELECT u.user_status,u.club_id,c.club_name FROM sys_user u JOIN club_info c ON c.club_id=u.club_id WHERE u.user_id=?",userId);
        assertThat(row.get("user_status")).isEqualTo("ENABLED");
        assertThat(row.get("club_id")).isNotNull();
        assertThat(row.get("club_name")).isEqualTo("P20B1银河足球俱乐部");
    }

    @Test
    void bindExistingAndLegacyApprovalEndpointAreRejected() throws Exception {
        String phone="13921000021";
        register(phone,"旧模式","李四","P20B1旧模式").andExpect(status().isOk());
        long userId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE phone=? AND role_id=(SELECT role_id FROM sys_role WHERE role_code='CLUB')",Long.class,phone);
        String admin=adminToken();
        mvc.perform(post("/api/admin/club-applications/{id}/approve",userId).header("Authorization","Bearer "+admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"BIND_EXISTING\",\"existingClubId\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users/{id}/club-approval",userId).header("Authorization","Bearer "+admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CREATE_NEW\"}"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT club_id FROM sys_user WHERE user_id=?",Long.class,userId)).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions register(String phone,String username,String realName,String clubName) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "username",username,"phone",phone,"password","safe123","roleCode","CLUB","realName",realName,"clubName",clubName))));
    }

    private String adminToken() throws Exception {
        String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone("13800000002","123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
