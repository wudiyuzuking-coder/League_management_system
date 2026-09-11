package com.example.leagueticket;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.TicketPassengerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class Phase22UserPassengerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TicketPassengerService passengerService;

    @Test
    void realMysqlHttpFlowEnforcesUserProfileAndPassengerLimitsWithoutPersistingFixtures() throws Exception {
        assertThat(TestTransaction.isActive()).isTrue();
        List<Long> userIds = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String phone = "1392299000" + i;
            JsonNode registered = body(mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "username", "phase22_tx_" + i,
                                    "phone", phone,
                                    "password", "safe123",
                                    "roleCode", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.realName").value("phase22_tx_" + i))
                    .andReturn().getResponse().getContentAsString());
            userIds.add(registered.path("data").path("userId").asLong());
            JsonNode loggedIn = body(mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "phone", phone, "password", "safe123", "roleCode", "USER"))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            tokens.add(loggedIn.path("data").path("token").asText());
        }

        mvc.perform(put("/api/users/me").header("Authorization", bearer(tokens.get(0)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "phase22_tx_renamed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("phase22_tx_renamed"))
                .andExpect(jsonPath("$.data.phone").value("13922990001"));
        mvc.perform(put("/api/users/me").header("Authorization", bearer(tokens.get(0)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", "phase22_tx_renamed", "phone", "13922999999"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("普通用户手机号不可修改"));

        List<Long> sharedAssociationIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            JsonNode response = addPassenger(tokens.get(i), "共享购票人" + i, "110101199001010011", 200);
            sharedAssociationIds.add(response.path("data").path("prefilledPassengerId").asLong());
        }
        addPassenger(tokens.get(4), "第五个用户", "110101199001010011", 409);
        addPassenger(tokens.get(0), "重复购票人", "110101199001010011", 409);

        List<Long> firstUserPassengerIds = new ArrayList<>();
        firstUserPassengerIds.add(sharedAssociationIds.get(0));
        String[] cards = {"11010119900101002X", "110101199001010038", "110101199001010046"};
        for (String card : cards) {
            JsonNode response = addPassenger(tokens.get(0), "个人购票人", card, 200);
            firstUserPassengerIds.add(response.path("data").path("prefilledPassengerId").asLong());
        }
        addPassenger(tokens.get(0), "第五名购票人", "110101199001010054", 409);

        assertThat(passengerService.requireForOrder(userIds.get(0), firstUserPassengerIds.subList(0, 2), 2))
                .hasSize(2);
        assertThatThrownBy(() -> passengerService.requireForOrder(
                userIds.get(0), firstUserPassengerIds.subList(0, 1), 2))
                .isInstanceOf(BusinessException.class)
                .hasMessage("购票人数量必须与购票张数完全一致");

        mvc.perform(delete("/api/user/prefilled-passengers/{id}", sharedAssociationIds.get(0))
                        .header("Authorization", bearer(tokens.get(0))))
                .andExpect(status().isOk());
        addPassenger(tokens.get(4), "释放后成功", "110101199001010011", 200);
        mvc.perform(get("/api/user/prefilled-passengers")
                        .header("Authorization", bearer(tokens.get(4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private JsonNode addPassenger(String token, String name, String card, int expectedStatus) throws Exception {
        String response = mvc.perform(post("/api/user/prefilled-passengers")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passengerName", name, "idCardNo", card))))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return body(response);
    }

    private JsonNode body(String value) throws Exception {
        return json.readTree(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
