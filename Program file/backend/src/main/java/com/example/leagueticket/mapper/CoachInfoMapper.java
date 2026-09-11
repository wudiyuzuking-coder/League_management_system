package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.CoachInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface CoachInfoMapper {
    @Select("SELECT * FROM coach_info WHERE coach_id=#{coachId} LIMIT 1")
    CoachInfo findById(Long coachId);

    @Select("SELECT * FROM coach_info WHERE club_id=#{clubId} ORDER BY coach_id")
    List<CoachInfo> findByClubId(Long clubId);

    @Select("SELECT * FROM coach_info WHERE club_id=#{clubId} AND coach_status='ACTIVE' ORDER BY coach_name,coach_id")
    List<CoachInfo> findActiveByClubId(Long clubId);

    @Insert("""
            INSERT INTO coach_info (club_id,coach_name,title,nationality,birth_year,description,coach_status)
            VALUES (#{clubId},#{coachName},#{title},#{nationality},#{birthYear},#{description},#{coachStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "coachId")
    int insert(CoachInfo coach);

    @Update("""
            UPDATE coach_info SET coach_name=#{coachName},title=#{title},nationality=#{nationality},birth_year=#{birthYear},description=#{description}
            WHERE coach_id=#{coachId}
            """)
    int update(CoachInfo coach);

    @Update("UPDATE coach_info SET title=#{title} WHERE coach_id=#{coachId}") int adjust(CoachInfo coach);
    @Select("SELECT COUNT(*) FROM coach_info WHERE club_id=#{clubId} AND coach_status='ACTIVE' AND title=#{title} AND (#{excludeId} IS NULL OR coach_id!=#{excludeId})")
    int countActiveTitle(@Param("clubId")Long clubId,@Param("title")String title,@Param("excludeId")Long excludeId);
    @Select("SELECT COUNT(*) FROM club_season_enrollment_coach WHERE coach_id=#{coachId}") int countEnrollmentSnapshots(Long coachId);
    @Delete("DELETE FROM coach_info WHERE coach_id=#{coachId} AND coach_status='INACTIVE'") int deleteLeft(Long coachId);

    @Update("UPDATE coach_info SET coach_status=#{status} WHERE coach_id=#{coachId}")
    int updateStatus(@Param("coachId") Long coachId, @Param("status") String status);
}
