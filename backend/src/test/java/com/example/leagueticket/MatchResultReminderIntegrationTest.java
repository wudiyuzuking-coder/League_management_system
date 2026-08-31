package com.example.leagueticket;

import com.example.leagueticket.service.SystemTimeService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class MatchResultReminderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired SystemTimeService systemTimeService;

    String eventAdmin,user,club,admin;
    long seasonId,roundId,clubA,clubB,stadiumA;

    @BeforeEach void setup() throws Exception {
        jdbc.update("INSERT INTO sys_config(config_key,config_value,value_type,description,config_status) VALUES('SYSTEM_TIME_OFFSET_SECONDS','0','INTEGER','test','ENABLED') ON DUPLICATE KEY UPDATE config_value='0',config_status='ENABLED'");
        cleanupData();
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,season_status) VALUES('IT16D赛季','2046-01-01','2046-12-31','ACTIVE')");
        seasonId=id("SELECT season_id FROM season_info WHERE season_name='IT16D赛季'");
        jdbc.update("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(?,1,'IT16D第1轮','2046-06-01','2046-06-30','PUBLISHED')",seasonId);
        roundId=id("SELECT round_id FROM round_info WHERE season_id="+seasonId);
        var clubs=jdbc.queryForList("SELECT club_id,home_stadium_id FROM club_info WHERE club_status='ACTIVE' AND home_stadium_id IS NOT NULL ORDER BY club_id LIMIT 2");
        clubA=((Number)clubs.get(0).get("club_id")).longValue();stadiumA=((Number)clubs.get(0).get("home_stadium_id")).longValue();clubB=((Number)clubs.get(1).get("club_id")).longValue();
        eventAdmin=login("demo_event_admin");user=login("demo_user");club=login("demo_club");admin=login("demo_admin");
    }

    @AfterEach void cleanup(){jdbc.update("UPDATE sys_config SET config_value='0',config_status='ENABLED' WHERE config_key='SYSTEM_TIME_OFFSET_SECONDS'");cleanupData();jdbc.update("DELETE FROM operation_log WHERE module_name='SYSTEM_TIME'");}

    @Test void futureDateRejectsWithoutWritingScoreOrStandings() throws Exception {
        long match=match("IN_PROGRESS","2046-06-10T19:30:00",null,null);
        setTime("2046-06-09T23:59:00");
        score(match,2,1).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("比赛日期尚未到达，暂不能录入比分"));
        assertThat(jdbc.queryForObject("SELECT home_score IS NULL AND away_score IS NULL FROM match_info WHERE match_id=?",Boolean.class,match)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_season_record WHERE season_id=?",Integer.class,seasonId)).isZero();
    }

    @Test void sameDateBeforeKickoffAndLaterDateAreAllowedAndRollbackOnlyBlocksNewAction() throws Exception {
        long today=match("IN_PROGRESS","2046-06-10T19:30:00",null,null);
        setTime("2046-06-10T00:01:00");score(today,0,0).andExpect(status().isOk());
        long past=match("IN_PROGRESS","2046-06-10T20:30:00",null,null);
        setTime("2046-06-11T08:00:00");score(past,3,2).andExpect(status().isOk());
        setTime("2046-06-09T08:00:00");score(past,1,1).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT CONCAT(home_score,':',away_score) FROM match_info WHERE match_id=?",String.class,past)).isEqualTo("3:2");
    }

    @Test void originalStatusRulesStillRejectDraftPublishedAndCancelled() throws Exception {
        setTime("2046-06-10T12:00:00");
        for(String state:new String[]{"DRAFT","PUBLISHED","CANCELLED"})score(match(state,"2046-06-10T10:00:00",null,null),1,0).andExpect(status().isBadRequest());
    }

    @Test void remindersClassifySortFilterAndExcludeNonActionableMatches() throws Exception {
        long overdue3=match("PUBLISHED","2046-06-07T20:00:00",null,null);
        long overdue1=match("IN_PROGRESS","2046-06-09T18:00:00",null,null);
        long today=match("PUBLISHED","2046-06-10T19:30:00",null,null);
        long future=match("PUBLISHED","2046-06-11T12:00:00",null,null);
        long draft=match("DRAFT","2046-06-08T12:00:00",null,null);
        long cancelled=match("CANCELLED","2046-06-08T13:00:00",null,null);
        long finished=match("FINISHED","2046-06-08T14:00:00",0,0);
        setTime("2046-06-10T09:00:00");
        mvc.perform(get("/api/admin/matches/result-reminders?size=20&seasonId="+seasonId).header("Authorization",bearer(eventAdmin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.records[*].matchId",contains((int)overdue3,(int)overdue1,(int)today)))
                .andExpect(jsonPath("$.data.records[0].reminderType").value("OVERDUE")).andExpect(jsonPath("$.data.records[0].daysOverdue").value(3))
                .andExpect(jsonPath("$.data.records[2].reminderType").value("TODAY")).andExpect(jsonPath("$.data.records[2].daysOverdue").value(0))
                .andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)future)))).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)draft))))
                .andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)cancelled)))).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)finished))));
        mvc.perform(get("/api/admin/matches/result-reminders?reminderType=TODAY&seasonId="+seasonId).header("Authorization",bearer(eventAdmin))).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/matches/result-reminders?reminderType=OVERDUE&seasonId="+seasonId).header("Authorization",bearer(eventAdmin))).andExpect(jsonPath("$.data.total").value(2));
    }

    @Test void finishingRemovesReminderAndRecalculatesStandings() throws Exception {
        long match=match("IN_PROGRESS","2046-06-10T19:30:00",null,null);setTime("2046-06-10T08:00:00");
        reminders(eventAdmin).andExpect(jsonPath("$.data.records[*].matchId",hasItem((int)match)));
        score(match,2,1).andExpect(status().isOk());
        mvc.perform(put("/api/admin/matches/{id}/status",match).header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content("{\"matchStatus\":\"FINISHED\"}")).andExpect(status().isOk());
        reminders(eventAdmin).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)match))));
        assertThat(jdbc.queryForObject("SELECT CONCAT(played,',',wins,',',draws,',',losses,',',goals_for,',',goals_against,',',points) FROM club_season_record WHERE season_id=? AND club_id=?",String.class,seasonId,clubA)).isEqualTo("1,1,0,0,2,1,3");
        assertThat(jdbc.queryForObject("SELECT CONCAT(played,',',wins,',',draws,',',losses,',',goals_for,',',goals_against,',',points) FROM club_season_record WHERE season_id=? AND club_id=?",String.class,seasonId,clubB)).isEqualTo("1,0,0,1,1,2,0");
    }

    @Test void systemTimeChangesRemindersImmediatelyAndOnlyEventAdminCanRead() throws Exception {
        long match=match("PUBLISHED","2046-06-10T19:30:00",null,null);
        setTime("2046-06-09T12:00:00");reminders(eventAdmin).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)match))));
        setTime("2046-06-10T12:00:00");reminders(eventAdmin).andExpect(status().isOk()).andExpect(jsonPath("$.data.records[*].matchId",hasItem((int)match)));
        setTime("2046-06-09T12:00:00");reminders(eventAdmin).andExpect(jsonPath("$.data.records[*].matchId",not(hasItem((int)match))));
        for(String token:new String[]{user,club,admin})reminders(token).andExpect(status().isForbidden());
    }

    private long match(String status,String time,Integer home,Integer away){jdbc.update("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,home_score,away_score,match_status) VALUES(?,?,?,?,?,?,?,?,?)",seasonId,roundId,clubA,clubB,stadiumA,LocalDateTime.parse(time),home,away,status);return id("SELECT MAX(match_id) FROM match_info WHERE season_id="+seasonId);}
    private ResultActions score(long id,int home,int away)throws Exception{return mvc.perform(put("/api/admin/matches/{id}/score",id).header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("homeScore",home,"awayScore",away))));}
    private ResultActions reminders(String token)throws Exception{return mvc.perform(get("/api/admin/matches/result-reminders?size=100&seasonId="+seasonId).header("Authorization",bearer(token)));}
    private void setTime(String time)throws Exception{mvc.perform(put("/api/system-time").header("Authorization",bearer(eventAdmin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("targetTime",LocalDateTime.parse(time))))).andExpect(status().isOk());}
    private String login(String username)throws Exception{JsonNode body=json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",username,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());return body.path("data").path("token").asText();}
    private void cleanupData(){jdbc.update("DELETE FROM club_season_record WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT16D赛季')");jdbc.update("DELETE FROM match_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT16D赛季')");jdbc.update("DELETE FROM round_info WHERE season_id IN (SELECT season_id FROM season_info WHERE season_name='IT16D赛季')");jdbc.update("DELETE FROM season_info WHERE season_name='IT16D赛季'");}
    private long id(String sql){return jdbc.queryForObject(sql,Long.class);}
    private static String bearer(String token){return "Bearer "+token;}
}
