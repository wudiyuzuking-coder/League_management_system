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
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class StadiumManagementIntegrationTest {
    @Autowired MockMvc mockMvc;@Autowired ObjectMapper objectMapper;@Autowired JdbcTemplate jdbc;
    String admin,systemAdmin,user,club;
    @BeforeEach void reset()throws Exception{jdbc.update("DELETE FROM stadium_seat WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'IT7%')");jdbc.update("DELETE FROM stadium_zone WHERE stadium_id IN (SELECT stadium_id FROM stadium_info WHERE stadium_name LIKE 'IT7%')");jdbc.update("DELETE FROM stadium_info WHERE stadium_name LIKE 'IT7%'");admin=loginByPhone("13800000005");systemAdmin=loginByPhone("13800000002");user=loginByPhone("13800000001");club=loginByPhone("13800000003");}

    @Test void stadiumCrudValidationAndPermission()throws Exception{
        long id=createStadium("IT7中心场",100);
        createStadiumRequest("IT7中心场",100,admin).andExpect(status().isConflict());
        createStadiumRequest("IT7零容量",0,admin).andExpect(status().isBadRequest());
        createStadiumRequest("IT7用户越权",100,user).andExpect(status().isForbidden());
        createStadiumRequest("IT7系统管理员越权",100,systemAdmin).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/stadiums/{id}",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(stadium("IT7中心场更新",120)))).andExpect(status().isOk()).andExpect(jsonPath("$.data.capacity").value(120));
        mockMvc.perform(put("/api/admin/stadiums/{id}/status",id).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"stadiumStatus\":\"DISABLED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.stadiumStatus").value("DISABLED"));
        mockMvc.perform(get("/api/stadiums").header("Authorization",bearer(club))).andExpect(status().isOk());
    }

    @Test void zoneUniquenessExistenceAndPermission()throws Exception{
        long stadiumId=createStadium("IT7票区场",200);long zoneId=createZone(stadiumId,"A","A区",1);
        createZoneRequest(stadiumId,"A","B区",2,admin).andExpect(status().isConflict());
        createZoneRequest(stadiumId,"B","A区",2,admin).andExpect(status().isConflict());
        createZoneRequest(999999L,"X","不存在",0,admin).andExpect(status().isNotFound());
        createZoneRequest(stadiumId,"B","B区",2,user).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/stadium-zones/{id}",zoneId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(zone("A2","A区更新",3)))).andExpect(status().isOk()).andExpect(jsonPath("$.data.sortNo").value(3));
    }

    @Test void seatsBatchLayoutStatusRollbackAndCapacity()throws Exception{
        long stadiumId=createStadium("IT7布局场",500);long zoneId=createZone(stadiumId,"MAIN","主看台",0);
        seatRequest(zoneId,0,"0排",1,"1",admin).andExpect(status().isBadRequest());seatRequest(zoneId,1,"1排",0,"0",admin).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/stadium-zones/{id}/seats/batch",zoneId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(batch())).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(5));
        long before=jdbc.queryForObject("SELECT COUNT(*) FROM stadium_seat WHERE stadium_zone_id=?",Long.class,zoneId);
        mockMvc.perform(post("/api/admin/stadium-zones/{id}/seats/batch",zoneId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(batch())).andExpect(status().isConflict());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stadium_seat WHERE stadium_zone_id=?",Long.class,zoneId)).isEqualTo(before);
        seatRequest(zoneId,1,"1排",1,"1",admin).andExpect(status().isConflict());
        mockMvc.perform(get("/api/stadium-zones/{id}/layout",zoneId).header("Authorization",bearer(user))).andExpect(status().isOk()).andExpect(jsonPath("$.data",hasSize(2))).andExpect(jsonPath("$.data[0].rowNo").value(1)).andExpect(jsonPath("$.data[0].seats",hasSize(2))).andExpect(jsonPath("$.data[1].rowNo").value(2)).andExpect(jsonPath("$.data[1].seats",hasSize(3))).andExpect(jsonPath("$.data[1].seats[0].seatNo").value(1));
        long seatId=jdbc.queryForObject("SELECT MIN(stadium_seat_id) FROM stadium_seat WHERE stadium_zone_id=?",Long.class,zoneId);
        mockMvc.perform(put("/api/admin/stadium-seats/{id}/status",seatId).header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"seatStatus\":\"DISABLED\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/stadiums/{id}/capacity-summary",stadiumId).header("Authorization",bearer(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.data.declaredCapacity").value(500)).andExpect(jsonPath("$.data.totalSeatCount").value(5)).andExpect(jsonPath("$.data.activeSeatCount").value(4)).andExpect(jsonPath("$.data.disabledSeatCount").value(1));
    }

    private long createStadium(String name,int capacity)throws Exception{String body=createStadiumRequest(name,capacity,admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("stadiumId").asLong();}
    private org.springframework.test.web.servlet.ResultActions createStadiumRequest(String name,int capacity,String token)throws Exception{return mockMvc.perform(post("/api/admin/stadiums").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(stadium(name,capacity))));}
    private Map<String,Object> stadium(String name,int capacity){return Map.of("stadiumName",name,"city","测试城","address","测试路1号","capacity",capacity,"layoutDesc","集成测试");}
    private long createZone(long stadiumId,String code,String name,int sort)throws Exception{String body=createZoneRequest(stadiumId,code,name,sort,admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(body).path("data").path("stadiumZoneId").asLong();}
    private org.springframework.test.web.servlet.ResultActions createZoneRequest(long stadiumId,String code,String name,int sort,String token)throws Exception{return mockMvc.perform(post("/api/admin/stadiums/{id}/zones",stadiumId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(zone(code,name,sort))));}
    private Map<String,Object> zone(String code,String name,int sort){return Map.of("zoneCode",code,"zoneName",name,"sortNo",sort,"description","test");}
    private org.springframework.test.web.servlet.ResultActions seatRequest(long zoneId,int rowNo,String rowLabel,int seatNo,String seatLabel,String token)throws Exception{return mockMvc.perform(post("/api/admin/stadium-zones/{id}/seats",zoneId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rowNo",rowNo,"rowLabel",rowLabel,"seatNo",seatNo,"seatLabel",seatLabel))));}
    private String batch()throws Exception{return json(Map.of("rows",List.of(Map.of("rowNo",2,"rowLabel","2排","startSeatNo",1,"seatCount",3),Map.of("rowNo",1,"rowLabel","1排","startSeatNo",1,"seatCount",2))));}
    private String loginByPhone(String phone)throws Exception{String body=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("phone",phone,"password","123456")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode n=objectMapper.readTree(body);return n.path("data").path("token").asText();}
    private String bearer(String token){return "Bearer "+token;}private String json(Object value)throws Exception{return objectMapper.writeValueAsString(value);}
}
