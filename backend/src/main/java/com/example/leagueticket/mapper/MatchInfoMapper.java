package com.example.leagueticket.mapper;

import com.example.leagueticket.dto.MatchQueryRequest;
import com.example.leagueticket.entity.MatchInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Mapper
public interface MatchInfoMapper {
    String JOIN_SELECT="""
        SELECT m.*,s.season_name,r.round_no,r.round_name,
          hc.club_name home_club_name,hc.logo_url home_logo_url,
          ac.club_name away_club_name,ac.logo_url away_logo_url,st.stadium_name
        FROM match_info m JOIN season_info s ON s.season_id=m.season_id
        JOIN round_info r ON r.round_id=m.round_id
        JOIN club_info hc ON hc.club_id=m.home_club_id
        JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        """;
    @Select(JOIN_SELECT+" WHERE m.match_id=#{id}") MatchInfo findById(Long id);
    @Select("""
        <script>SELECT COUNT(*) FROM match_info m <where>
        <if test='q.seasonId!=null'>AND m.season_id=#{q.seasonId}</if><if test='q.roundId!=null'>AND m.round_id=#{q.roundId}</if>
        <if test='q.homeClubId!=null'>AND m.home_club_id=#{q.homeClubId}</if><if test='q.awayClubId!=null'>AND m.away_club_id=#{q.awayClubId}</if>
        <if test='q.clubId!=null'>AND (m.home_club_id=#{q.clubId} OR m.away_club_id=#{q.clubId})</if>
        <if test='q.matchStatus!=null and q.matchStatus!=""'>AND m.match_status=#{q.matchStatus}</if>
        <if test='q.publicOnly==true'>AND m.match_status IN ('PUBLISHED','IN_PROGRESS','FINISHED')</if>
        <if test='q.startTime!=null'>AND m.match_time&gt;=#{q.startTime}</if><if test='q.endTime!=null'>AND m.match_time&lt;=#{q.endTime}</if>
        </where></script>
        """) long count(@Param("q") MatchQueryRequest query);
    String PAGE_SELECT="""
        <script>
        SELECT m.*,s.season_name,r.round_no,r.round_name,
          hc.club_name home_club_name,hc.logo_url home_logo_url,
          ac.club_name away_club_name,ac.logo_url away_logo_url,st.stadium_name
        FROM match_info m JOIN season_info s ON s.season_id=m.season_id
        JOIN round_info r ON r.round_id=m.round_id
        JOIN club_info hc ON hc.club_id=m.home_club_id
        JOIN club_info ac ON ac.club_id=m.away_club_id
        JOIN stadium_info st ON st.stadium_id=m.stadium_id
        <where>
        <if test='q.seasonId!=null'>AND m.season_id=#{q.seasonId}</if><if test='q.roundId!=null'>AND m.round_id=#{q.roundId}</if>
        <if test='q.homeClubId!=null'>AND m.home_club_id=#{q.homeClubId}</if><if test='q.awayClubId!=null'>AND m.away_club_id=#{q.awayClubId}</if>
        <if test='q.clubId!=null'>AND (m.home_club_id=#{q.clubId} OR m.away_club_id=#{q.clubId})</if>
        <if test='q.matchStatus!=null and q.matchStatus!=""'>AND m.match_status=#{q.matchStatus}</if>
        <if test='q.publicOnly==true'>AND m.match_status IN ('PUBLISHED','IN_PROGRESS','FINISHED')</if>
        <if test='q.startTime!=null'>AND m.match_time&gt;=#{q.startTime}</if><if test='q.endTime!=null'>AND m.match_time&lt;=#{q.endTime}</if>
        </where> ORDER BY m.match_time,m.match_id LIMIT #{limit} OFFSET #{offset}</script>
        """;
    @Select(PAGE_SELECT) List<MatchInfo> findPage(@Param("q") MatchQueryRequest query,@Param("offset") long offset,@Param("limit") int limit);
    @Select("SELECT COUNT(*) FROM match_info WHERE season_id=#{seasonId} AND round_id=#{roundId} AND home_club_id=#{homeClubId} AND away_club_id=#{awayClubId} AND (#{excludeId} IS NULL OR match_id!=#{excludeId})")
    int countDuplicate(@Param("seasonId") Long seasonId,@Param("roundId") Long roundId,@Param("homeClubId") Long homeClubId,@Param("awayClubId") Long awayClubId,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO match_info(season_id,round_id,home_club_id,away_club_id,stadium_id,match_time,match_status,published_at) VALUES(#{seasonId},#{roundId},#{homeClubId},#{awayClubId},#{stadiumId},#{matchTime},'DRAFT',NULL)")
    @Options(useGeneratedKeys=true,keyProperty="matchId") int insert(MatchInfo match);
    @Update("UPDATE match_info SET season_id=#{seasonId},round_id=#{roundId},home_club_id=#{homeClubId},away_club_id=#{awayClubId},stadium_id=#{stadiumId},match_time=#{matchTime} WHERE match_id=#{matchId}") int updateBasic(MatchInfo match);
    @Update("UPDATE match_info SET match_time=#{matchTime} WHERE match_id=#{matchId}") int updateTime(MatchInfo match);
    @Update("UPDATE match_info SET match_status='PUBLISHED',published_at=COALESCE(published_at,#{now}) WHERE match_id=#{id}") int publish(@Param("id")Long id,@Param("now")LocalDateTime now);
    @Update("UPDATE match_info SET match_status=#{status} WHERE match_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
    @Update("UPDATE match_info SET home_score=#{homeScore},away_score=#{awayScore} WHERE match_id=#{id}") int updateScore(@Param("id") Long id,@Param("homeScore") Integer homeScore,@Param("awayScore") Integer awayScore);
    @Select("SELECT match_id,season_id,home_club_id,away_club_id,home_score,away_score FROM match_info WHERE season_id=#{seasonId} AND match_status='FINISHED' AND home_score IS NOT NULL AND away_score IS NOT NULL ORDER BY match_id") List<MatchInfo> findFinishedBySeason(Long seasonId);
    @Select("""
        <script>SELECT COUNT(*) FROM match_info m WHERE m.match_status IN ('PUBLISHED','IN_PROGRESS')
        AND DATE(m.match_time)&lt;=#{systemDate}
        <if test='seasonId!=null'>AND m.season_id=#{seasonId}</if>
        <if test='reminderType=="TODAY"'>AND DATE(m.match_time)=#{systemDate}</if>
        <if test='reminderType=="OVERDUE"'>AND DATE(m.match_time)&lt;#{systemDate}</if>
        </script>
        """) long countResultReminders(@Param("systemDate") LocalDate systemDate,@Param("seasonId") Long seasonId,@Param("reminderType") String reminderType);
    @Select("""
        <script>"""+JOIN_SELECT+"""
        WHERE m.match_status IN ('PUBLISHED','IN_PROGRESS') AND DATE(m.match_time)&lt;=#{systemDate}
        <if test='seasonId!=null'>AND m.season_id=#{seasonId}</if>
        <if test='reminderType=="TODAY"'>AND DATE(m.match_time)=#{systemDate}</if>
        <if test='reminderType=="OVERDUE"'>AND DATE(m.match_time)&lt;#{systemDate}</if>
        ORDER BY CASE WHEN DATE(m.match_time)&lt;#{systemDate} THEN 0 ELSE 1 END,m.match_time,m.match_id
        LIMIT #{limit} OFFSET #{offset}</script>
        """) List<MatchInfo> findResultReminders(@Param("systemDate") LocalDate systemDate,@Param("seasonId") Long seasonId,
        @Param("reminderType") String reminderType,@Param("offset") long offset,@Param("limit") int limit);
    @Select("""
        <script>"""+JOIN_SELECT+"""
        WHERE 1=1
        <if test='homeClubId!=null'>AND m.home_club_id=#{homeClubId}</if>
        <if test='status!=null'>AND m.match_status=#{status}</if>
        <if test='startTime!=null'>AND m.match_time&gt;=#{startTime}</if>
        <if test='endTime!=null'>AND m.match_time&lt;#{endTime}</if>
        ORDER BY m.match_time,m.match_id</script>
        """) List<MatchInfo> findCheckerMatches(@Param("homeClubId")Long homeClubId,
        @Param("status")String status,@Param("startTime")java.time.LocalDateTime startTime,
        @Param("endTime")java.time.LocalDateTime endTime);
}
