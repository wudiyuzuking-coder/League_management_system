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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Duration;
import java.time.LocalDateTime;
import com.example.leagueticket.service.SystemTimeService;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class MatchManagementIntegrationTest {
    @Autowired MockMvc mockMvc; @Autowired ObjectMapper objectMapper; @Autowired JdbcTemplate jdbc; @Autowired SystemTimeService systemTimeService;
    String admin,systemAdmin,user,club; long seasonId,otherSeasonId,roundId,otherRoundId,clubA,clubB,clubC,stadiumA,stadiumB;

    @BeforeEach void reset() throws Exception {
        long offset=Duration.between(systemTimeService.realNow(),LocalDateTime.of(2036,2,3,12,0)).getSeconds();
        jdbc.update("INSERT INTO sys_config(config_key,config_value,value_type,description,config_status) VALUES('SYSTEM_TIME_OFFSET_SECONDS',?,'INTEGER','test','ENABLED') ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),config_status='ENABLED'",Long.toString(offset));
        jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT6%')");
        jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT6%')");
        jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name LIKE 'IT6%')");
        jdbc.update("DELETE FROM season_info WHERE season_name LIKE 'IT6%'");
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT6主赛季','2036-01-01','2036-12-31','ACTIVE')");seasonId=id("SELECT season_id FROM season_info WHERE season_name='IT6主赛季'");
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT6其他赛季','2037-01-01','2037-12-31','ACTIVE')");otherSeasonId=id("SELECT season_id FROM season_info WHERE season_name='IT6其他赛季'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT6第1轮','2036-02-01','2036-02-03','PUBLISHED')",seasonId);roundId=id("SELECT round_id FROM round_info WHERE season_id="+seasonId+" AND round_no=1");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT6其他轮','2037-02-01','2037-02-03','PUBLISHED')",otherSeasonId);otherRoundId=id("SELECT round_id FROM round_info WHERE season_id="+otherSeasonId+" AND round_no=1");
        var clubs=jdbc.queryForList("SELECT club_id,home_stadium_id FROM club_info WHERE home_stadium_id IS NOT NULL ORDER BY club_id LIMIT 3");
        clubA=((Number)clubs.get(0).get("club_id")).longValue();stadiumA=((Number)clubs.get(0).get("home_stadium_id")).longValue();clubB=((Number)clubs.get(1).get("club_id")).longValue();stadiumB=((Number)clubs.get(1).get("home_stadium_id")).longValue();clubC=((Number)clubs.get(2).get("club_id")).longValue();
        admin=login("demo_event_admin");systemAdmin=login("demo_admin");user=login("demo_user");club=login("demo_club");
    }

    @AfterEach void resetSystemTime(){jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");}

    @Test void createValidationFilteringAndPermissions() throws Exception {
        long matchId=create(seasonId,roundId,clubA,clubB,stadiumA,"2036-02-01T19:30:00");
        mockMvc.perform(get("/api/matches?clubId="+clubA).header("Authorization",bearer(club))).andExpect(status().isOk()).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)matchId))));
        mockMvc.perform(get("/api/matches/{id}",matchId).header("Authorization",bearer(user))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/matches?seasonId="+seasonId+"&clubId="+clubA).header("Authorization",bearer(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.data.records[*].matchId",hasItem((int)matchId)));
        createRequest(seasonId,roundId,clubA,clubC,stadiumA,"2036-02-02T19:30:00",user).andExpect(status().isForbidden());
        createRequest(seasonId,roundId,clubA,clubC,stadiumA,"2036-02-02T19:30:00",systemAdmin).andExpect(status().isForbidden());
        createRequest(seasonId,roundId,clubA,clubA,stadiumA,"2036-02-02T19:30:00",admin).andExpect(status().isBadRequest());
        createRequest(seasonId,otherRoundId,clubA,clubC,stadiumA,"2036-02-02T19:30:00",admin).andExpect(status().isBadRequest());
        createRequest(seasonId,roundId,clubA,clubC,stadiumA,"2036-02-10T19:30:00",admin).andExpect(status().isBadRequest());
        createRequest(seasonId,roundId,clubA,clubC,stadiumB,"2036-02-02T19:30:00",admin).andExpect(status().isBadRequest());
        createRequest(seasonId,roundId,clubA,clubB,stadiumA,"2036-02-02T20:00:00",admin).andExpect(status().isConflict());
    }

    @Test void editStatusPublishedTimeAndScoreRules() throws Exception {
        long id=create(seasonId,roundId,clubA,clubB,stadiumA,"2036-02-01T18:00:00");
        update(id,seasonId,roundId,clubA,clubB,stadiumA,"2036-02-01T19:00:00",admin).andExpect(status().isOk());
        transition(id,"FINISHED").andExpect(status().isBadRequest());
        transition(id,"PUBLISHED").andExpect(status().isOk()).andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
        String first=jdbc.queryForObject("SELECT DATE_FORMAT(published_at,'%Y-%m-%d %H:%i:%s') FROM match_info WHERE match_id=?",String.class,id);
        transition(id,"PUBLISHED").andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT DATE_FORMAT(published_at,'%Y-%m-%d %H:%i:%s') FROM match_info WHERE match_id=?",String.class,id)).isEqualTo(first);
        update(id,seasonId,roundId,clubA,clubB,stadiumA,"2036-02-01T20:00:00",admin).andExpect(status().isOk());
        update(id,seasonId,roundId,clubA,clubC,stadiumA,"2036-02-01T20:00:00",admin).andExpect(status().isBadRequest());
        score(id,1,0,admin).andExpect(status().isBadRequest());
        transition(id,"IN_PROGRESS").andExpect(status().isOk());
        transition(id,"FINISHED").andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/matches/{id}/score",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"homeScore\":1}")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/matches/{id}/score",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"homeScore\":-1,\"awayScore\":0}")).andExpect(status().isBadRequest());
        score(id,1,0,user).andExpect(status().isForbidden());score(id,1,0,admin).andExpect(status().isOk());
        transition(id,"FINISHED").andExpect(status().isOk());transition(id,"IN_PROGRESS").andExpect(status().isBadRequest());
        update(id,seasonId,roundId,clubA,clubB,stadiumA,"2036-02-02T20:00:00",admin).andExpect(status().isBadRequest());
        long cancel=create(seasonId,roundId,clubB,clubA,stadiumB,"2036-02-02T18:00:00");transition(cancel,"CANCELLED").andExpect(status().isOk());transition(cancel,"DRAFT").andExpect(status().isBadRequest());
    }

    @Test void finishedMatchesRecalculateAndCorrectStandings() throws Exception {
        long ab=create(seasonId,roundId,clubA,clubB,stadiumA,"2036-02-01T12:00:00");
        long ac=create(seasonId,roundId,clubA,clubC,stadiumA,"2036-02-02T12:00:00");
        long bc=create(seasonId,roundId,clubB,clubC,stadiumB,"2036-02-03T12:00:00");
        finish(ab,2,0);finish(ac,1,1);finish(bc,3,1);
        assertRecord(clubA,2,1,1,0,3,1,4);assertRecord(clubB,2,1,0,1,3,3,3);assertRecord(clubC,2,0,1,1,2,4,1);
        score(ab,0,1,admin).andExpect(status().isOk());
        assertRecord(clubA,2,0,1,1,1,2,1);assertRecord(clubB,2,2,0,0,4,1,6);assertRecord(clubC,2,0,1,1,2,4,1);
        mockMvc.perform(get("/api/seasons/{id}/standings",seasonId).header("Authorization",bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].goalDifference",hasItem(-2)));
    }

    private void finish(long id,int home,int away)throws Exception{transition(id,"PUBLISHED").andExpect(status().isOk());transition(id,"IN_PROGRESS").andExpect(status().isOk());score(id,home,away,admin).andExpect(status().isOk());transition(id,"FINISHED").andExpect(status().isOk());}
    private void assertRecord(long clubId,int played,int wins,int draws,int losses,int gf,int ga,int points){Map<String,Object> r=jdbc.queryForMap("SELECT played,wins,draws,losses,goals_for,goals_against,points FROM club_season_record WHERE season_id=? AND club_id=?",seasonId,clubId);org.assertj.core.api.Assertions.assertThat(((Number)r.get("played")).intValue()).isEqualTo(played);org.assertj.core.api.Assertions.assertThat(((Number)r.get("wins")).intValue()).isEqualTo(wins);org.assertj.core.api.Assertions.assertThat(((Number)r.get("draws")).intValue()).isEqualTo(draws);org.assertj.core.api.Assertions.assertThat(((Number)r.get("losses")).intValue()).isEqualTo(losses);org.assertj.core.api.Assertions.assertThat(((Number)r.get("goals_for")).intValue()).isEqualTo(gf);org.assertj.core.api.Assertions.assertThat(((Number)r.get("goals_against")).intValue()).isEqualTo(ga);org.assertj.core.api.Assertions.assertThat(((Number)r.get("points")).intValue()).isEqualTo(points);}
    private long create(long s,long r,long h,long a,long st,String time)throws Exception{String body=createRequest(s,r,h,a,st,time,admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("matchId").asLong();}
    private org.springframework.test.web.servlet.ResultActions createRequest(long s,long r,long h,long a,long st,String time,String token)throws Exception{return mockMvc.perform(post("/api/admin/matches").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(match(s,r,h,a,st,time))));}
    private org.springframework.test.web.servlet.ResultActions update(long id,long s,long r,long h,long a,long st,String time,String token)throws Exception{return mockMvc.perform(put("/api/admin/matches/{id}",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(match(s,r,h,a,st,time))));}
    private org.springframework.test.web.servlet.ResultActions transition(long id,String value)throws Exception{return mockMvc.perform(put("/api/admin/matches/{id}/status",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("matchStatus",value))));}
    private org.springframework.test.web.servlet.ResultActions score(long id,Integer home,Integer away,String token)throws Exception{return mockMvc.perform(put("/api/admin/matches/{id}/score",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("homeScore",home,"awayScore",away))));}
    private Map<String,Object> match(long s,long r,long h,long a,long st,String time){Map<String,Object> m=new LinkedHashMap<>();m.put("seasonId",s);m.put("roundId",r);m.put("homeClubId",h);m.put("awayClubId",a);m.put("stadiumId",st);m.put("matchTime",time);return m;}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private String login(String username)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username",username,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;} private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
