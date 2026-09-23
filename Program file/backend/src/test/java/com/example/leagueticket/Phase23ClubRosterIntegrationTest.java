package com.example.leagueticket;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.EnrollmentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class Phase23ClubRosterIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ClubHomeStadiumService homeService;
    @Autowired PlayerInfoService playerService;
    @Autowired ClubPersonnelService personnelService;
    @Autowired ClubSeasonEnrollmentService enrollmentService;
    @Autowired ClubLogoService logoService;
    @Autowired ClubDataService clubDataService;
    @Autowired SystemTimeService timeService;

    @Test void standardHomeRosterLifecycleAndEnrollmentSnapshotStayConsistent() {
        long clubId=jdbc.queryForObject("SELECT club_id FROM club_info WHERE club_status='ACTIVE' ORDER BY club_id LIMIT 1",Long.class);
        var old=homeService.profile(clubId);
        String stadium="IT23标准主场-"+System.nanoTime();
        var home=new HomeStadiumRequest("验收城",stadium,"验收路1号",1,3,2,new BigDecimal("120.00"),new BigDecimal("60.00"));
        var profile=homeService.updateProfile(clubId,new ClubProfileRequest(old.clubName(),old.shortName(),null,old.description(),home));
        assertThat(profile.venueModel()).isEqualTo("STANDARD_8");
        assertThat(profile.capacity()).isEqualTo(20);
        assertThat(profile.standardHomeComplete()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stadium_zone WHERE stadium_id=?",Integer.class,profile.homeStadiumId())).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stadium_seat WHERE stadium_id=?",Integer.class,profile.homeStadiumId())).isEqualTo(20);
        byte[] png=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        var logo=logoService.upload(clubId,new MockMultipartFile("file","club.png","image/png",png));
        assertThat(logo.avatarUrl()).startsWith("/uploads/club-logos/").endsWith(".png");
        assertThat(jdbc.queryForObject("SELECT logo_url FROM club_info WHERE club_id=?",String.class,clubId)).isEqualTo(logo.avatarUrl());

        List<PlayerInfo> players=playerService.listByClub(clubId).stream().filter(p->"ACTIVE".equals(p.getPlayerStatus())).limit(11).toList();
        assertThat(players).hasSize(11);
        for(PlayerInfo player:players){String position=players.indexOf(player)==0?"GOALKEEPER":("GOALKEEPER".equals(player.getPosition())?"DEFENDER":player.getPosition());playerService.adjust(clubId,player.getPlayerId(),new PlayerAdjustRequest(player.getShirtNo(),position,"STARTER"));}
        assertThat(personnelService.overview(clubId).compliant()).isTrue();

        PlayerInfo departed=playerService.create(clubId,new PlayerRequest("IT23号码释放",99,"FORWARD","中国",null,2001,"SUBSTITUTE"));
        playerService.updateStatus(clubId,departed.getPlayerId(),"TRANSFERRED");
        PlayerInfo returned=playerService.returnToTeam(clubId,departed.getPlayerId(),new PlayerReturnRequest(98,"FORWARD","SUBSTITUTE"));
        assertThat(returned.getShirtNo()).isEqualTo(98);

        LocalDateTime now=timeService.now();String seasonName="IT23快照-"+System.nanoTime();
        jdbc.update("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status) VALUES(?,?,?,?,?,4,'REGISTRATION')",seasonName,now.toLocalDate().plusDays(30),now.toLocalDate().plusDays(90),now.minusDays(1),now.plusDays(10));
        long seasonId=jdbc.queryForObject("SELECT season_id FROM season_info WHERE season_name=?",Long.class,seasonName);
        EnrollmentResponse enrollment=enrollmentService.submit(clubId,new EnrollmentRequest(seasonId));
        assertThat(enrollment.getPlayerCount()).isEqualTo(12);
        assertThat(enrollment.getPlayers()).hasSize(12);
        String savedName=enrollment.getPlayers().get(0).playerName();
        PlayerInfo first=players.get(0);
        playerService.adjust(clubId,first.getPlayerId(),new PlayerAdjustRequest(first.getShirtNo(),"DEFENDER","STARTER"));
        assertThat(enrollmentService.detailClub(clubId,enrollment.getEnrollmentId()).getPlayers().get(0).playerName()).isEqualTo(savedName);

        long matchId=jdbc.queryForObject("SELECT match_id FROM match_info WHERE home_club_id=? ORDER BY match_id LIMIT 1",Long.class,clubId);
        long matchZoneId=jdbc.queryForObject("SELECT match_zone_id FROM match_ticket_zone WHERE match_id=? ORDER BY match_zone_id LIMIT 1",Long.class,matchId);
        long inventoryId=jdbc.queryForObject("SELECT inventory_id FROM match_seat_inventory WHERE match_id=? AND match_zone_id=? ORDER BY inventory_id LIMIT 1",Long.class,matchId,matchZoneId);
        long userId=jdbc.queryForObject("SELECT u.user_id FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE r.role_code='USER' ORDER BY u.user_id LIMIT 1",Long.class);
        jdbc.update("UPDATE match_info SET match_status='FINISHED',home_score=2,away_score=1 WHERE match_id=?",matchId);
        String orderNo="IT23"+System.nanoTime();
        jdbc.update("INSERT INTO ticket_order(order_no,user_id,match_id,match_zone_id,ticket_count,total_amount,order_status,expire_time,paid_at) VALUES(?,?,?,?,1,100,'PAID',?,?)",orderNo,userId,matchId,matchZoneId,now.plusMinutes(15),now);
        long orderId=jdbc.queryForObject("SELECT order_id FROM ticket_order WHERE order_no=?",Long.class,orderNo);
        jdbc.update("INSERT INTO order_item(order_id,inventory_id,ticket_price,zone_name_snapshot,row_no_snapshot,seat_no_snapshot,item_status) VALUES(?,?,100,'IT23','1','1','PAID')",orderId,inventoryId);
        jdbc.update("UPDATE match_seat_inventory SET inventory_status='SOLD' WHERE inventory_id=?",inventoryId);
        var clubData=clubDataService.get(clubId);
        assertThat(clubData.revenue()).isEqualByComparingTo("60.00");
        assertThat(clubData.performance().getPoints()).isEqualTo(3);
        assertThat(clubData.rankings().get(0).getClubId()).isEqualTo(clubId);
    }
}
