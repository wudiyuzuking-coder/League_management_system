package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.CoachInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CoachInfoMapper {
    @Select("SELECT * FROM coach_info WHERE coach_id=#{coachId} LIMIT 1")
    CoachInfo findById(Long coachId);

    @Select("SELECT * FROM coach_info WHERE club_id=#{clubId} ORDER BY coach_id")
    List<CoachInfo> findByClubId(Long clubId);

    @Insert("""
            INSERT INTO coach_info (club_id,coach_name,title,nationality,description,coach_status)
            VALUES (#{clubId},#{coachName},#{title},#{nationality},#{description},#{coachStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "coachId")
    int insert(CoachInfo coach);

    @Update("""
            UPDATE coach_info SET coach_name=#{coachName},title=#{title},nationality=#{nationality},description=#{description}
            WHERE coach_id=#{coachId}
            """)
    int update(CoachInfo coach);

    @Update("UPDATE coach_info SET coach_status=#{status} WHERE coach_id=#{coachId}")
    int updateStatus(@Param("coachId") Long coachId, @Param("status") String status);
}
