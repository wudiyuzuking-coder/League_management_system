package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.PlayerInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PlayerInfoMapper {
    @Select("SELECT * FROM player_info WHERE player_id=#{playerId} LIMIT 1")
    PlayerInfo findById(Long playerId);

    @Select("SELECT * FROM player_info WHERE club_id=#{clubId} ORDER BY shirt_no,player_id")
    List<PlayerInfo> findByClubId(Long clubId);

    @Select("SELECT COUNT(*) FROM player_info WHERE club_id=#{clubId} AND shirt_no=#{shirtNo} AND (#{excludeId} IS NULL OR player_id != #{excludeId})")
    int countShirtNo(@Param("clubId") Long clubId, @Param("shirtNo") Integer shirtNo, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO player_info
              (club_id,player_name,shirt_no,position,nationality,birth_date,player_status)
            VALUES
              (#{clubId},#{playerName},#{shirtNo},#{position},#{nationality},#{birthDate},#{playerStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "playerId")
    int insert(PlayerInfo player);

    @Update("""
            UPDATE player_info SET player_name=#{playerName},shirt_no=#{shirtNo},position=#{position},
              nationality=#{nationality},birth_date=#{birthDate} WHERE player_id=#{playerId}
            """)
    int update(PlayerInfo player);

    @Update("UPDATE player_info SET player_status=#{status} WHERE player_id=#{playerId}")
    int updateStatus(@Param("playerId") Long playerId, @Param("status") String status);
}
