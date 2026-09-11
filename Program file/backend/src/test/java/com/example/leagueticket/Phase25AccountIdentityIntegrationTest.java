package com.example.leagueticket;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev") @Transactional
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class Phase25AccountIdentityIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    String admin;
    @BeforeEach void loginAdmin() throws Exception {admin=token(login("13800000002","ADMIN","0001","123456").andExpect(status().isOk()));}

    @Test void samePhoneAcrossRolesIsAllowedButSameRoleDuplicateAndRoleMismatchAreRejected() throws Exception {
        String phone="13925000001";
        register(phone,"USER","共享用户",null,null).andExpect(status().isOk()).andExpect(jsonPath("$.data.userStatus").value("ENABLED"));
        register(phone,"CLUB","共享负责人","共享负责人","Phase25共享号码俱乐部").andExpect(status().isOk()).andExpect(jsonPath("$.data.userStatus").value("PENDING_CLUB_APPROVAL"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE phone=?",Integer.class,phone)).isEqualTo(2);
        login(phone,"USER",null,"safe123").andExpect(status().isOk());
        login(phone,"CLUB",null,"safe123").andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("俱乐部审核中，请先以普通用户身份进入"));
        register(phone,"USER","重复用户",null,null).andExpect(status().isConflict());
        login(phone,"EVENT_ADMIN","0001","safe123").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("所选身份与账号不匹配"));
        login("13925999999","USER",null,"safe123").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("该账号未注册"));
    }

    @Test void internalPreRegistrationActivationDisableAndDomainBoundariesAreEnforced() throws Exception {
        String phone="13925000002";
        String created=mvc.perform(post("/api/admin/internal-users").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"realName","首次启用管理员","employeeNo","4321")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.employeeNo").value("EA4321")).andExpect(jsonPath("$.data.userStatus").value("PENDING_ACTIVATION")).andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).path("data").path("userId").asLong();
        mvc.perform(post("/api/auth/management/probe").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"employeeNo","4321")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.activationRequired").value(true));
        mvc.perform(post("/api/auth/management/activate").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"employeeNo","4321","realName","错误姓名","password","safe123","confirmPassword","safe123")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/management/activate").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"employeeNo","4321","realName","首次启用管理员","password","safe123","confirmPassword","safe123")))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/management/activate").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"employeeNo","4321","realName","首次启用管理员","password","safe123","confirmPassword","safe123")))).andExpect(status().isConflict());
        login(phone,"EVENT_ADMIN","4321","safe123").andExpect(status().isOk());
        mvc.perform(put("/api/admin/internal-users/{id}/status",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"userStatus\":\"DISABLED\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/management/probe").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("roleCode","EVENT_ADMIN","phone",phone,"employeeNo","4321")))).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("该工号已被停用，请联系管理员"));
        mvc.perform(get("/api/admin/users").param("roleCode","CLUB").header("Authorization",bearer(admin))).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/{id}/status",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"userStatus\":\"ENABLED\"}")).andExpect(status().isForbidden());
        long userId=jdbc.queryForObject("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE r.role_code='USER' LIMIT 1",Long.class);
        mvc.perform(put("/api/admin/internal-users/{id}/status",userId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"userStatus\":\"DISABLED\"}")).andExpect(status().isForbidden());
    }

    @Test void clubApprovalOnlyCreatesNewClubAndLegacyBindModeIsRejected() throws Exception {
        String response=register("13925000003","CLUB","新负责人","新负责人","Phase25新申请俱乐部").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();long id=json.readTree(response).path("data").path("userId").asLong();
        mvc.perform(post("/api/admin/club-applications/{id}/approve",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"BIND_EXISTING\",\"existingClubId\":1}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users/{id}/club-approval",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CREATE_NEW\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/club-applications/{id}/approve",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CREATE_NEW\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.userStatus").value("ENABLED"));
        assertThat(jdbc.queryForObject("SELECT c.club_name FROM club_info c JOIN sys_user u ON u.club_id=c.club_id WHERE u.user_id=?",String.class,id)).isEqualTo("Phase25新申请俱乐部");
    }

    private ResultActions register(String phone,String role,String username,String realName,String clubName)throws Exception{Map<String,Object> p=new LinkedHashMap<>();p.put("phone",phone);p.put("roleCode",role);p.put("username",username);p.put("password","safe123");if(realName!=null)p.put("realName",realName);if(clubName!=null)p.put("clubName",clubName);return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(p)));}
    private ResultActions login(String phone,String role,String employee,String password)throws Exception{Map<String,Object> p=new LinkedHashMap<>();p.put("phone",phone);p.put("roleCode",role);p.put("password",password);if(employee!=null)p.put("employeeNo",employee);return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(p)));}
    private String token(ResultActions result)throws Exception{return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data").path("token").asText();}
    private String body(Object value)throws Exception{return json.writeValueAsString(value);}private String bearer(String token){return "Bearer "+token;}
}
