package com.example.leagueticket.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SpaForwardController()).build();
    }

    @Test
    void forwardsNestedVueHistoryRoutes() throws Exception {
        mockMvc.perform(get("/user/orders/28"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/user/tickets/25"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void doesNotCatchApiUploadsOrAssets() throws Exception {
        mockMvc.perform(get("/api/not-a-real-endpoint"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/uploads/not-a-real-file.png"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/assets/not-a-real-file.js"))
                .andExpect(status().isNotFound());
    }
}
