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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Management account tests aligned with the Phase25 pre-registration/first-activation flow. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class ManagementEmployeeNoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void internalCreationBuildsPrefixAndStartsPendingWithoutFormalPassword() throws Exception {
        String admin=adminToken();
        mvc.perform(post("/api/admin/internal-users").header("Authorization","Bearer "+admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "phone","13917000001","realName","张三","employeeNo","1701","roleCode","EVENT_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("张三"))
                .andExpect(jsonPath("$.data.employeeNo").value("EA1701"))
                .andExpect(jsonPath("$.data.userStatus").value("PENDING_ACTIVATION"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE employee_no='EA1701' AND user_status='PENDING_ACTIVATION'",Integer.class)).isEqualTo(1);
    }

    @Test
    void employeeDigitsAreFourAndGloballyUnique() throws Exception {
        String admin=adminToken();
        create(admin,"13917000011","甲","1702","EVENT_ADMIN").andExpect(status().isOk());
        create(admin,"13917000012","乙","1702","EVENT_ADMIN").andExpect(status().isConflict());
        create(admin,"13917000013","丙","017","ADMIN").andExpect(status().isBadRequest());
        create(admin,"13917000014","丁","EA17","EVENT_ADMIN").andExpect(status().isBadRequest());
    }

    @Test
    void domainsRejectCrossRoleOperationsAndLegacyCreateEndpointIsGone() throws Exception {
        String admin=adminToken();
        mvc.perform(get("/api/admin/internal-users").queryParam("roleCode","USER").header("Authorization","Bearer "+admin))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").header("Authorization","Bearer "+admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private org.springframework.test.web.servlet.ResultActions create(String token,String phone,String name,String digits,String role) throws Exception {
        return mvc.perform(post("/api/admin/internal-users").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "phone",phone,"realName",name,"employeeNo",digits,"roleCode",role))));
    }

    private String adminToken() throws Exception {
        String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone("13800000002","123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
