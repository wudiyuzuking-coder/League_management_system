package com.example.leagueticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class ClubManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void adminClubDomainAllowsViewAndStatusOnly() throws Exception {
        String admin=login("13800000002");
        mvc.perform(get("/api/admin/clubs").header("Authorization",bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records").isArray());
        mvc.perform(get("/api/admin/clubs/1").header("Authorization",bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.leaderPhone").exists());
        mvc.perform(put("/api/admin/clubs/1/status").header("Authorization",bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clubStatus\":\"DISABLED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void oldAdminClubAndPersonnelMutationsAreGone() throws Exception {
        String admin=login("13800000002");
        mvc.perform(post("/api/admin/clubs").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/admin/clubs/1").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/admin/clubs/1/players").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/clubs/1/coaches").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    private String login(String phone) throws Exception {
        String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone(phone,"123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
    private static String bearer(String token){return "Bearer "+token;}
}
