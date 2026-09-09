package com.example.leagueticket.mapper;

import com.example.leagueticket.dto.ScheduleQueryRequest;
import com.example.leagueticket.entity.SeasonScheduleBatch;
import com.example.leagueticket.vo.ClubScheduleResponse;
import com.example.leagueticket.vo.ScheduleMatchResponse;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeasonScheduleMapper {
    @Select("SELECT b.*,s.season_name FROM season_schedule_batch b JOIN season_info s ON s.season_id=b.season_id WHERE b.season_id=#{seasonId}")
    SeasonScheduleBatch findBySeason(Long seasonId);
    @Select("SELECT COUNT(*) FROM season_schedule_batch WHERE season_id=#{seasonId}") int countBySeason(Long seasonId);
    @Insert("INSERT INTO season_schedule_batch(season_id,batch_status,trigger_type,club_count,round_count,match_count,generated_at) VALUES(#{seasonId},'GENERATED',#{triggerType},#{clubCount},#{roundCount},#{matchCount},#{generatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="batchId") int insertBatch(SeasonScheduleBatch batch);
    @Insert("INSERT INTO season_schedule_match(batch_id,match_id) VALUES(#{batchId},#{matchId})") int insertMatchLink(@Param("batchId")Long batchId,@Param("matchId")Long matchId);
    @Update("UPDATE season_schedule_batch SET batch_status='CONFIRMED',confirmed_at=#{now},confirmed_by=#{userId} WHERE season_id=#{seasonId} AND batch_status='GENERATED'")
    int confirm(@Param("seasonId")Long seasonId,@Param("userId")Long userId,@Param("now")LocalDateTime now);

    @Select("SELECT COUNT(*) FROM match_info WHERE season_id=#{seasonId}") int countSeasonMatches(Long seasonId);
    @Select("SELECT e.club_id,c.club_name,e.stadium_id,st.stadium_name FROM club_season_enrollment e JOIN club_info c ON c.club_id=e.club_id JOIN stadium_info st ON st.stadium_id=e.stadium_id WHERE e.season_id=#{seasonId} AND e.enrollment_status='SUBMITTED' ORDER BY e.club_id")
    List<EnrollmentTeam> findTeams(Long seasonId);
    @Select("SELECT season_id FROM season_info s WHERE s.season_status='DRAFT' AND s.registration_deadline IS NOT NULL AND s.registration_deadline<=#{now} AND NOT EXISTS(SELECT 1 FROM season_schedule_batch b WHERE b.season_id=s.season_id) ORDER BY s.season_id")
    List<Long> findDeadlineCandidates(LocalDateTime now);

    @Select("SELECT m.match_id,r.round_no,m.match_time match_date_time,m.home_club_id,h.club_name home_club_name,m.away_club_id,a.club_name away_club_name,m.stadium_id,st.stadium_name,m.match_status FROM season_schedule_match sm JOIN match_info m ON m.match_id=sm.match_id JOIN round_info r ON r.round_id=m.round_id JOIN club_info h ON h.club_id=m.home_club_id JOIN club_info a ON a.club_id=m.away_club_id JOIN stadium_info st ON st.stadium_id=m.stadium_id WHERE sm.batch_id=#{batchId} ORDER BY r.round_no,m.match_id")
    List<ScheduleMatchResponse> findBatchMatches(Long batchId);

    @Select("""
        <script>SELECT COUNT(*) FROM season_schedule_batch b <where>
        <if test='q.seasonId!=null'>AND b.season_id=#{q.seasonId}</if>
        <if test='q.batchStatus!=null and q.batchStatus!=""'>AND b.batch_status=#{q.batchStatus}</if>
        </where></script>
        """)
    long countPage(@Param("q")ScheduleQueryRequest q);
    @Select("""
        <script>SELECT b.*,s.season_name FROM season_schedule_batch b JOIN season_info s ON s.season_id=b.season_id <where>
        <if test='q.seasonId!=null'>AND b.season_id=#{q.seasonId}</if>
        <if test='q.batchStatus!=null and q.batchStatus!=""'>AND b.batch_status=#{q.batchStatus}</if>
        </where> ORDER BY b.generated_at DESC,b.batch_id DESC LIMIT #{limit} OFFSET #{offset}</script>
        """)
    List<SeasonScheduleBatch> findPage(@Param("q")ScheduleQueryRequest q,@Param("offset")long offset,@Param("limit")int limit);

    @Select("SELECT m.match_id,m.season_id,s.season_name,r.round_no,(m.home_club_id=#{clubId}) home,CASE WHEN m.home_club_id=#{clubId} THEN m.away_club_id ELSE m.home_club_id END opponent_club_id,CASE WHEN m.home_club_id=#{clubId} THEN a.club_name ELSE h.club_name END opponent_club_name,m.match_time match_date_time,m.stadium_id,st.stadium_name FROM season_schedule_batch b JOIN season_schedule_match sm ON sm.batch_id=b.batch_id JOIN match_info m ON m.match_id=sm.match_id JOIN season_info s ON s.season_id=m.season_id JOIN round_info r ON r.round_id=m.round_id JOIN club_info h ON h.club_id=m.home_club_id JOIN club_info a ON a.club_id=m.away_club_id JOIN stadium_info st ON st.stadium_id=m.stadium_id WHERE b.batch_status='CONFIRMED' AND (m.home_club_id=#{clubId} OR m.away_club_id=#{clubId}) ORDER BY m.match_time,m.match_id")
    List<ClubScheduleResponse> findConfirmedForClub(Long clubId);

    class EnrollmentTeam {
        private Long clubId; private String clubName; private Long stadiumId; private String stadiumName;
        public Long getClubId(){return clubId;} public void setClubId(Long v){clubId=v;}
        public String getClubName(){return clubName;} public void setClubName(String v){clubName=v;}
        public Long getStadiumId(){return stadiumId;} public void setStadiumId(Long v){stadiumId=v;}
        public String getStadiumName(){return stadiumName;} public void setStadiumName(String v){stadiumName=v;}
    }
}
