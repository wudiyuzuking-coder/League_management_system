package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.PlayerInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface PlayerInfoMapper {
    @Select("SELECT * FROM player_info WHERE player_id=#{playerId} LIMIT 1")
    PlayerInfo findById(Long playerId);

    @Select("SELECT * FROM player_info WHERE club_id=#{clubId} ORDER BY player_id")
    List<PlayerInfo> findByClubId(Long clubId);

    @Select("SELECT * FROM player_info WHERE club_id=#{clubId} AND player_status='ACTIVE' " +
            "ORDER BY shirt_no IS NULL,shirt_no,player_name,player_id")
    List<PlayerInfo> findActiveByClubId(Long clubId);

    @Select("SELECT COUNT(*) FROM player_info WHERE club_id=#{clubId} AND player_status!='TRANSFERRED' AND shirt_no=#{shirtNo} AND (#{excludeId} IS NULL OR player_id != #{excludeId})")
    int countShirtNo(@Param("clubId") Long clubId, @Param("shirtNo") Integer shirtNo, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO player_info
              (club_id,player_name,shirt_no,position,nationality,birth_date,birth_year,player_status,lineup_role)
            VALUES
              (#{clubId},#{playerName},#{shirtNo},#{position},#{nationality},#{birthDate},#{birthYear},#{playerStatus},#{lineupRole})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "playerId")
    int insert(PlayerInfo player);

    @Update("""
            UPDATE player_info SET player_name=#{playerName},shirt_no=#{shirtNo},position=#{position},
              nationality=#{nationality},birth_date=#{birthDate},birth_year=#{birthYear},lineup_role=#{lineupRole} WHERE player_id=#{playerId}
            """)
    int update(PlayerInfo player);

    @Update("UPDATE player_info SET shirt_no=#{shirtNo},position=#{position},lineup_role=#{lineupRole} WHERE player_id=#{playerId}")
    int adjust(PlayerInfo player);

    @Update("UPDATE player_info SET player_status='TRANSFERRED',shirt_no=NULL WHERE player_id=#{playerId}")
    int markLeft(Long playerId);

    @Update("UPDATE player_info SET player_status='ACTIVE',shirt_no=#{shirtNo},position=#{position},lineup_role=#{lineupRole} WHERE player_id=#{playerId}")
    int returnToTeam(PlayerInfo player);

    @Select("SELECT COUNT(*) FROM player_info WHERE club_id=#{clubId} AND player_status='ACTIVE' AND lineup_role=#{lineupRole} AND (#{excludeId} IS NULL OR player_id!=#{excludeId})")
    int countActiveRole(@Param("clubId")Long clubId,@Param("lineupRole")String lineupRole,@Param("excludeId")Long excludeId);

    @Select("SELECT COUNT(*) FROM player_info WHERE club_id=#{clubId} AND player_status='ACTIVE' AND lineup_role='STARTER' AND position='GOALKEEPER' AND (#{excludeId} IS NULL OR player_id!=#{excludeId})")
    int countStartingGoalkeepers(@Param("clubId")Long clubId,@Param("excludeId")Long excludeId);

    @Select("SELECT COUNT(*) FROM club_season_enrollment_player WHERE player_id=#{playerId}")
    int countEnrollmentSnapshots(Long playerId);

    @Delete("DELETE FROM player_info WHERE player_id=#{playerId} AND player_status='TRANSFERRED'")
    int deleteLeft(Long playerId);

    @Update("UPDATE player_info SET player_status=#{status} WHERE player_id=#{playerId}")
    int updateStatus(@Param("playerId") Long playerId, @Param("status") String status);
}
