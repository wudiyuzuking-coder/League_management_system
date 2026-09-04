package com.example.leagueticket.mapper;

import com.example.leagueticket.dto.EnrollmentQueryRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.vo.AvailableSeasonResponse;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ClubSeasonEnrollmentMapper {
    String SUMMARY="""
        SELECT e.enrollment_id,e.season_id,s.season_name,s.start_date,s.end_date,
          s.registration_start_time,s.registration_deadline,s.max_clubs,
          e.club_id,c.club_name,e.stadium_id,st.stadium_name,e.enrollment_status,e.submitted_at,
          (SELECT COUNT(*) FROM club_season_enrollment_player ep WHERE ep.enrollment_id=e.enrollment_id) player_count,
          (SELECT COUNT(*) FROM club_season_enrollment_coach ec WHERE ec.enrollment_id=e.enrollment_id) coach_count,
          (SELECT MIN(m.match_time) FROM match_info m WHERE m.season_id=e.season_id
             AND (m.home_club_id=e.club_id OR m.away_club_id=e.club_id) AND m.match_time>=#{now}) next_match_time
        FROM club_season_enrollment e
        JOIN season_info s ON s.season_id=e.season_id
        JOIN club_info c ON c.club_id=e.club_id
        JOIN stadium_info st ON st.stadium_id=e.stadium_id
        """;

    @Select("""
        SELECT s.season_id,s.season_name,s.start_date,s.end_date,s.registration_start_time,
          s.registration_deadline,s.max_clubs,
          (SELECT COUNT(*) FROM club_season_enrollment e WHERE e.season_id=s.season_id AND e.enrollment_status='SUBMITTED') enrolled_clubs,
          s.max_clubs-(SELECT COUNT(*) FROM club_season_enrollment e WHERE e.season_id=s.season_id AND e.enrollment_status='SUBMITTED') remaining_slots,
          #{now} system_time
        FROM season_info s
        WHERE s.season_status='DRAFT' AND s.registration_start_time IS NOT NULL
          AND s.registration_deadline IS NOT NULL AND s.max_clubs IS NOT NULL
          AND s.registration_start_time<=#{now} AND #{now}<s.registration_deadline
          AND NOT EXISTS (SELECT 1 FROM season_schedule_batch b WHERE b.season_id=s.season_id)
          AND (SELECT COUNT(*) FROM club_season_enrollment e WHERE e.season_id=s.season_id AND e.enrollment_status='SUBMITTED')<s.max_clubs
          AND NOT EXISTS (SELECT 1 FROM club_season_enrollment own WHERE own.season_id=s.season_id AND own.club_id=#{clubId})
          AND NOT EXISTS (SELECT 1 FROM club_season_enrollment own
              JOIN season_info os ON os.season_id=own.season_id
              WHERE own.club_id=#{clubId} AND own.enrollment_status='SUBMITTED'
                AND s.start_date<=os.end_date AND s.end_date>=os.start_date)
        ORDER BY s.start_date,s.season_id
        """)
    List<AvailableSeasonResponse> findAvailable(@Param("clubId")Long clubId,@Param("now")LocalDateTime now);

    @Select("SELECT COUNT(*) FROM club_season_enrollment WHERE season_id=#{seasonId} AND enrollment_status='SUBMITTED'")
    int countSubmitted(Long seasonId);
    @Select("SELECT enrollment_id FROM club_season_enrollment WHERE season_id=#{seasonId} AND enrollment_status='SUBMITTED' FOR UPDATE")
    List<Long> findSubmittedIdsForUpdate(Long seasonId);
    @Select("SELECT COUNT(*) FROM club_season_enrollment WHERE season_id=#{seasonId} AND club_id=#{clubId}")
    int countBySeasonClub(@Param("seasonId")Long seasonId,@Param("clubId")Long clubId);
    @Select("""
        SELECT s.* FROM club_season_enrollment e JOIN season_info s ON s.season_id=e.season_id
        WHERE e.club_id=#{clubId} AND e.enrollment_status='SUBMITTED' AND e.season_id<>#{seasonId}
          AND #{startDate}<=s.end_date AND #{endDate}>=s.start_date ORDER BY s.start_date LIMIT 1
        """)
    SeasonInfo findConflict(@Param("clubId")Long clubId,@Param("seasonId")Long seasonId,
        @Param("startDate")java.time.LocalDate startDate,@Param("endDate")java.time.LocalDate endDate);

    @Select("SELECT COUNT(*) FROM stadium_zone WHERE stadium_id=#{stadiumId} AND zone_status='ACTIVE'") int countActiveZones(Long stadiumId);
    @Select("SELECT COUNT(*) FROM stadium_seat WHERE stadium_id=#{stadiumId} AND seat_status='ACTIVE'") int countActiveSeats(Long stadiumId);

    @Insert("INSERT INTO club_season_enrollment(season_id,club_id,stadium_id,enrollment_status,submitted_at) VALUES(#{seasonId},#{clubId},#{stadiumId},#{enrollmentStatus},#{submittedAt})")
    @Options(useGeneratedKeys=true,keyProperty="enrollmentId") int insert(ClubSeasonEnrollment enrollment);
    @Insert("INSERT INTO club_season_enrollment_player(enrollment_id,player_id,lineup_role,player_name_snapshot,shirt_no_snapshot,position_snapshot,birth_date_snapshot) VALUES(#{enrollmentId},#{playerId},#{lineupRole},#{playerNameSnapshot},#{shirtNoSnapshot},#{positionSnapshot},#{birthDateSnapshot})")
    int insertPlayer(ClubSeasonEnrollmentPlayer player);
    @Insert("INSERT INTO club_season_enrollment_coach(enrollment_id,coach_id,coach_name_snapshot,title_snapshot) VALUES(#{enrollmentId},#{coachId},#{coachNameSnapshot},#{titleSnapshot})")
    int insertCoach(ClubSeasonEnrollmentCoach coach);

    @Select(SUMMARY+" WHERE e.enrollment_id=#{id}") ClubSeasonEnrollment findById(@Param("id")Long id,@Param("now")LocalDateTime now);
    @Select(SUMMARY+" WHERE e.club_id=#{clubId} ORDER BY e.submitted_at DESC,e.enrollment_id DESC")
    List<ClubSeasonEnrollment> findByClub(@Param("clubId")Long clubId,@Param("now")LocalDateTime now);
    @Select("SELECT * FROM club_season_enrollment_player WHERE enrollment_id=#{id} ORDER BY lineup_role,shirt_no_snapshot,player_id") List<ClubSeasonEnrollmentPlayer> findPlayers(Long id);
    @Select("SELECT * FROM club_season_enrollment_coach WHERE enrollment_id=#{id} ORDER BY coach_id") List<ClubSeasonEnrollmentCoach> findCoaches(Long id);

    @Select("""
        <script>SELECT COUNT(*) FROM club_season_enrollment e <where>
          <if test='q.seasonId!=null'>AND e.season_id=#{q.seasonId}</if>
          <if test='q.clubId!=null'>AND e.club_id=#{q.clubId}</if>
          <if test='q.enrollmentStatus!=null and q.enrollmentStatus!=""'>AND e.enrollment_status=#{q.enrollmentStatus}</if>
        </where></script>
        """) long countAdmin(@Param("q")EnrollmentQueryRequest query);
    @Select("""
        <script>"""+SUMMARY+"""
        <where>
          <if test='q.seasonId!=null'>AND e.season_id=#{q.seasonId}</if>
          <if test='q.clubId!=null'>AND e.club_id=#{q.clubId}</if>
          <if test='q.enrollmentStatus!=null and q.enrollmentStatus!=""'>AND e.enrollment_status=#{q.enrollmentStatus}</if>
        </where> ORDER BY e.submitted_at DESC,e.enrollment_id DESC LIMIT #{limit} OFFSET #{offset}</script>
        """) List<ClubSeasonEnrollment> findAdminPage(@Param("q")EnrollmentQueryRequest query,@Param("now")LocalDateTime now,
        @Param("offset")long offset,@Param("limit")int limit);
}
