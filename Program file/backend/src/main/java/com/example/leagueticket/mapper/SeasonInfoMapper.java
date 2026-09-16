package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.vo.PublicSeasonResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeasonInfoMapper {
    String PUBLIC_VISIBILITY_PREDICATE = """
        (EXISTS (SELECT 1 FROM season_schedule_batch visible_batch
            WHERE visible_batch.season_id=s.season_id AND visible_batch.batch_status='CONFIRMED')
          OR EXISTS (SELECT 1 FROM match_info visible_match
            WHERE visible_match.season_id=s.season_id
              AND visible_match.match_status IN ('IN_PROGRESS','FINISHED')))
        """;
    String PUBLIC_SEASON_SELECT = """
        SELECT s.season_id,s.season_name,s.start_date,s.end_date,
          COALESCE(b.club_count,teams.team_count,0) team_count,
          COALESCE(b.round_count,matches.actual_round_count,0) round_count,
          COALESCE(b.match_count,matches.actual_match_count,0) match_count,
          CASE WHEN b.batch_status='CONFIRMED' THEN TRUE ELSE FALSE END schedule_confirmed,
          CASE
            WHEN COALESCE(matches.actual_match_count,0)>0
              AND COALESCE(matches.finished_count,0)=matches.actual_match_count THEN 'FINISHED'
            WHEN COALESCE(matches.in_progress_count,0)>0 OR COALESCE(matches.finished_count,0)>0 THEN 'IN_PROGRESS'
            WHEN b.batch_status='CONFIRMED' THEN 'SCHEDULE_PUBLISHED'
            ELSE 'REGISTRATION'
          END public_status
        FROM season_info s
        LEFT JOIN season_schedule_batch b ON b.season_id=s.season_id
        LEFT JOIN (
          SELECT season_id,COUNT(*) actual_match_count,COUNT(DISTINCT round_id) actual_round_count,
            SUM(match_status='IN_PROGRESS') in_progress_count,SUM(match_status='FINISHED') finished_count
          FROM match_info GROUP BY season_id
        ) matches ON matches.season_id=s.season_id
        LEFT JOIN (
          SELECT season_id,COUNT(DISTINCT club_id) team_count
          FROM (
            SELECT season_id,home_club_id club_id FROM match_info
            UNION ALL
            SELECT season_id,away_club_id club_id FROM match_info
          ) participants GROUP BY season_id
        ) teams ON teams.season_id=s.season_id
        WHERE """ + PUBLIC_VISIBILITY_PREDICATE;

    @Select("""
        SELECT s.*,
          (SELECT COUNT(*) FROM club_season_enrollment e
            WHERE e.season_id=s.season_id AND e.enrollment_status='SUBMITTED') submitted_team_count,
          (SELECT b.batch_status FROM season_schedule_batch b
            WHERE b.season_id=s.season_id LIMIT 1) schedule_batch_status
        FROM season_info s ORDER BY s.start_date DESC,s.season_id DESC
        """) List<SeasonInfo> findAll();
    @Select(PUBLIC_SEASON_SELECT + " ORDER BY s.start_date DESC,s.season_id DESC")
    List<PublicSeasonResponse> findPublic();
    @Select(PUBLIC_SEASON_SELECT + " AND s.season_id=#{id}")
    PublicSeasonResponse findPublicById(Long id);
    @Select("SELECT EXISTS(SELECT 1 FROM season_info s WHERE s.season_id=#{id} AND " + PUBLIC_VISIBILITY_PREDICATE + ")")
    boolean isPublicVisible(Long id);
    @Select("SELECT * FROM season_info WHERE season_id=#{id}") SeasonInfo findById(Long id);
    @Select("SELECT * FROM season_info WHERE season_id=#{id} FOR UPDATE") SeasonInfo findByIdForUpdate(Long id);
    @Select("""
        SELECT s.* FROM season_info s
        WHERE s.season_status IN ('ACTIVE','FINISHED')
          AND #{systemDate} BETWEEN s.start_date AND s.end_date
          AND (EXISTS (SELECT 1 FROM club_season_record r WHERE r.season_id=s.season_id AND r.club_id=#{clubId})
            OR EXISTS (SELECT 1 FROM match_info m WHERE m.season_id=s.season_id
              AND (m.home_club_id=#{clubId} OR m.away_club_id=#{clubId})
              AND m.match_status IN ('PUBLISHED','IN_PROGRESS','FINISHED')))
        ORDER BY CASE s.season_status WHEN 'ACTIVE' THEN 0 ELSE 1 END,s.start_date DESC,s.season_id DESC LIMIT 1
        """) SeasonInfo findCurrentPublicForClub(@Param("clubId") Long clubId,
                                                   @Param("systemDate") java.time.LocalDate systemDate);
    @Select("""
        SELECT s.* FROM season_info s
        WHERE s.season_status IN ('ACTIVE','FINISHED')
          AND (EXISTS (SELECT 1 FROM club_season_record r WHERE r.season_id=s.season_id AND r.club_id=#{clubId})
            OR EXISTS (SELECT 1 FROM match_info m WHERE m.season_id=s.season_id
              AND (m.home_club_id=#{clubId} OR m.away_club_id=#{clubId})
              AND m.match_status IN ('PUBLISHED','IN_PROGRESS','FINISHED')))
        ORDER BY s.start_date DESC,s.season_id DESC LIMIT 1
        """) SeasonInfo findLatestPublicForClub(Long clubId);
    @Select("SELECT COUNT(*) FROM season_info WHERE season_name=#{name} AND (#{excludeId} IS NULL OR season_id!=#{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
    @Insert("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,ticket_sale_start_time,max_clubs,season_status,description) VALUES(#{seasonName},#{startDate},#{endDate},#{registrationStartTime},#{registrationDeadline},#{ticketSaleStartTime},#{maxClubs},#{seasonStatus},#{description})")
    @Options(useGeneratedKeys=true,keyProperty="seasonId") int insert(SeasonInfo season);
    @Update("UPDATE season_info SET season_name=#{seasonName},start_date=#{startDate},end_date=#{endDate},registration_start_time=#{registrationStartTime},registration_deadline=#{registrationDeadline},ticket_sale_start_time=#{ticketSaleStartTime},max_clubs=#{maxClubs} WHERE season_id=#{seasonId}") int update(SeasonInfo season);
    @Update("UPDATE season_info SET season_status=#{status} WHERE season_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
}
