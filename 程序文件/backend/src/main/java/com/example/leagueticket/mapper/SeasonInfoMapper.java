package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SeasonInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeasonInfoMapper {
    @Select("SELECT * FROM season_info ORDER BY start_date DESC, season_id DESC") List<SeasonInfo> findAll();
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
    @Insert("INSERT INTO season_info(season_name,start_date,end_date,registration_start_time,registration_deadline,max_clubs,season_status,description) VALUES(#{seasonName},#{startDate},#{endDate},#{registrationStartTime},#{registrationDeadline},#{maxClubs},#{seasonStatus},#{description})")
    @Options(useGeneratedKeys=true,keyProperty="seasonId") int insert(SeasonInfo season);
    @Update("UPDATE season_info SET season_name=#{seasonName},start_date=#{startDate},end_date=#{endDate},registration_start_time=#{registrationStartTime},registration_deadline=#{registrationDeadline},max_clubs=#{maxClubs},description=#{description} WHERE season_id=#{seasonId}") int update(SeasonInfo season);
    @Update("UPDATE season_info SET season_status=#{status} WHERE season_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
}
