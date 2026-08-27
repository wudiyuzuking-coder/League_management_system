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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class LeagueManagementIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    String adminToken,userToken,clubToken;

    @BeforeEach void reset() throws Exception {
        jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT5%')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT5%')");
        jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'IT5%'");
        adminToken=login("demo_admin");userToken=login("demo_user");clubToken=login("demo_club");
    }

    @Test void seasonValidationAuthorizationAndStateMachine() throws Exception {
        long id=createSeason("IT5赛季状态");
        mockMvc.perform(post("/api/admin/seasons").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(season("IT5赛季状态","2035-01-01","2035-12-31")))).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/seasons").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(season("IT5错误日期","2035-12-31","2035-01-01")))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/seasons").header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(season("IT5用户越权","2035-01-01","2035-12-31")))).andExpect(status().isForbidden());
        statusSeason(id,"ACTIVE").andExpect(status().isOk()).andExpect(jsonPath("$.data.seasonStatus").value("ACTIVE"));
        statusSeason(id,"DRAFT").andExpect(status().isBadRequest());
        statusSeason(id,"FINISHED").andExpect(status().isOk());
        statusSeason(id,"ACTIVE").andExpect(status().isBadRequest());
    }

    @Test void roundValidationAuthorizationAndStateMachine() throws Exception {
        long seasonId=createSeason("IT5轮次赛季");
        long roundId=createRound(seasonId,1,"2035-02-01","2035-02-02");
        mockMvc.perform(post("/api/admin/seasons/{id}/rounds",seasonId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(round(1,"2035-03-01","2035-03-02")))).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/seasons/{id}/rounds",seasonId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(round(2,"2034-12-30","2035-01-02")))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/seasons/{id}/rounds",seasonId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(round(2,"2035-03-03","2035-03-01")))).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/rounds/{id}",roundId).header("Authorization",bearer(userToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json(round(1,"2035-02-01","2035-02-02")))).andExpect(status().isForbidden());
        statusRound(roundId,"PUBLISHED").andExpect(status().isOk());
        statusRound(roundId,"DRAFT").andExpect(status().isBadRequest());
        statusRound(roundId,"FINISHED").andExpect(status().isOk());
    }

    @Test void standingsAreInitializedCalculatedSortedAndProtected() throws Exception {
        long seasonId=createSeason("IT5积分榜赛季");
        mockMvc.perform(post("/api/admin/seasons/{id}/standings/init",seasonId).header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data",greaterThan(0)));
        mockMvc.perform(post("/api/admin/seasons/{id}/standings/init",seasonId).header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(0));
        var ids=jdbc.queryForList("SELECT record_id FROM club_season_record WHERE season_id=? ORDER BY club_id LIMIT 3",Long.class,seasonId);
        updateRecord(ids.get(0),2,0,0,5,1,adminToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.points").value(6)).andExpect(jsonPath("$.data.matchesPlayed").value(2)).andExpect(jsonPath("$.data.goalDifference").value(4));
        updateRecord(ids.get(1),2,0,0,4,0,adminToken).andExpect(status().isOk());
        updateRecord(ids.get(2),2,0,0,6,2,adminToken).andExpect(status().isOk());
        mockMvc.perform(get("/api/seasons/{id}/standings",seasonId).header("Authorization",bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].recordId").value(ids.get(2)))
                .andExpect(jsonPath("$.data[1].recordId").value(ids.get(0))).andExpect(jsonPath("$.data[2].recordId").value(ids.get(1)));
        mockMvc.perform(put("/api/admin/season-records/{id}",ids.get(0)).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"wins\":-1,\"draws\":0,\"losses\":0,\"goalsFor\":0,\"goalsAgainst\":0}")).andExpect(status().isBadRequest());
        updateRecord(ids.get(0),0,0,0,0,0,clubToken).andExpect(status().isForbidden());
    }

    private long createSeason(String name)throws Exception{String body=mockMvc.perform(post("/api/admin/seasons").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content(json(season(name,"2035-01-01","2035-12-31")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("seasonId").asLong();}
    private long createRound(long seasonId,int no,String start,String end)throws Exception{String body=mockMvc.perform(post("/api/admin/seasons/{id}/rounds",seasonId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content(json(round(no,start,end)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("roundId").asLong();}
    private org.springframework.test.web.servlet.ResultActions statusSeason(long id,String status)throws Exception{return mockMvc.perform(put("/api/admin/seasons/{id}/status",id).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("seasonStatus",status))));}
    private org.springframework.test.web.servlet.ResultActions statusRound(long id,String status)throws Exception{return mockMvc.perform(put("/api/admin/rounds/{id}/status",id).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("roundStatus",status))));}
    private org.springframework.test.web.servlet.ResultActions updateRecord(long id,int w,int d,int l,int gf,int ga,String token)throws Exception{return mockMvc.perform(put("/api/admin/season-records/{id}",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("wins",w,"draws",d,"losses",l,"goalsFor",gf,"goalsAgainst",ga))));}
    private Map<String,Object> season(String name,String start,String end){return Map.of("seasonName",name,"startDate",start,"endDate",end,"description","integration");}
    private Map<String,Object> round(int no,String start,String end){return Map.of("roundNo",no,"roundName","第"+no+"轮","startDate",start,"endDate",end);}
    private String login(String username)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username",username,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}
    private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
