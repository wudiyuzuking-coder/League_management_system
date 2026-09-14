package com.example.leagueticket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev") @Transactional
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class AccountCancellationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;

    @Test void userCancellationRejectsLoginAndImmediatelyInvalidatesOldToken() throws Exception {
        long id=insert("13928000001","USER",null,null);
        String token=login("13928000001","USER",null);
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("账号已注销"));
        assertThat(jdbc.queryForObject("SELECT user_status FROM sys_user WHERE user_id=?",String.class,id)).isEqualTo("CANCELLED");
        mvc.perform(get("/api/auth/me").header("Authorization",bearer(token))).andExpect(status().isUnauthorized());
        loginRequest("13928000001","USER",null).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("该账号已注销"));
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test void boundClubIsRejectedWithoutChangingRelationship() throws Exception {
        String clubName="注销保护测试俱乐部"+System.nanoTime();
        jdbc.update("INSERT INTO club_info(club_name,club_status) VALUES(?,'ACTIVE')",clubName);
        Long clubId=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_name=?",Long.class,clubName);
        long id=insert("13928000002","CLUB",null,clubId);
        String token=login("13928000002","CLUB",null);
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(token)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("当前账号仍为俱乐部负责人，无法直接注销，请联系系统管理员处理俱乐部负责人关系"));
        assertThat(jdbc.queryForMap("SELECT user_status,club_id FROM sys_user WHERE user_id=?",id)).containsEntry("user_status","ENABLED").containsEntry("club_id",clubId);
    }

    @Test void eventAdminCancellationPreservesResultSubmission() throws Exception {
        long id=insert("13928000003","EVENT_ADMIN","EA9280",null);
        Long matchId=jdbc.queryForObject("SELECT match_id FROM match_info ORDER BY match_id LIMIT 1",Long.class);
        jdbc.update("INSERT INTO match_result_submission(match_id,event_admin_id,home_score,away_score,submitted_at) VALUES(?,?,1,0,NOW())",matchId,id);
        String token=login("13928000003","EVENT_ADMIN","9280");
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(token))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_result_submission WHERE event_admin_id=?",Integer.class,id)).isEqualTo(1);
    }

    @Test void nonLastAdminCanCancelButLastEnabledAdminCannot() throws Exception {
        long first=insert("13928000004","ADMIN","SA9281",null);
        long last=insert("13928000005","ADMIN","SA9282",null);
        String firstToken=login("13928000004","ADMIN","9281");
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(firstToken))).andExpect(status().isOk());
        jdbc.update("UPDATE sys_user u JOIN sys_role r ON r.role_id=u.role_id SET u.user_status='DISABLED' WHERE r.role_code='ADMIN' AND u.user_id<>?",last);
        String lastToken=login("13928000005","ADMIN","9282");
        mvc.perform(post("/api/account/cancel").header("Authorization",bearer(lastToken)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("当前账号是最后一个可用系统管理员，无法注销"));
        assertThat(jdbc.queryForObject("SELECT user_status FROM sys_user WHERE user_id=?",String.class,last)).isEqualTo("ENABLED");
    }

    private long insert(String phone,String role,String employee,Long clubId){
        jdbc.update("INSERT INTO sys_user(username,phone,password_hash,display_name,employee_no,role_id,club_id,user_status) SELECT ?,?,?,?, ?,role_id,?,'ENABLED' FROM sys_role WHERE role_code=?",role+phone,phone,encoder.encode("safe123"),role+"注销测试",employee,clubId,role);
        return jdbc.queryForObject("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE u.phone=? AND r.role_code=?",Long.class,phone,role);
    }
    private String login(String phone,String role,String employee)throws Exception{return json.readTree(loginRequest(phone,role,employee).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("token").asText();}
    private org.springframework.test.web.servlet.ResultActions loginRequest(String phone,String role,String employee)throws Exception{var body=new java.util.LinkedHashMap<String,Object>();body.put("phone",phone);body.put("roleCode",role);body.put("password","safe123");if(employee!=null)body.put("employeeNo",employee);return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));}
    private String bearer(String token){return "Bearer "+token;}
}
