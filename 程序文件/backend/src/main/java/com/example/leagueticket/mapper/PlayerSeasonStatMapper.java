package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.PlayerSeasonStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PlayerSeasonStatMapper {
    String COLUMNS = "s.stat_id,s.season_id,se.season_name,s.player_id,p.player_name,s.club_id," +
            "s.appearances,s.starts,s.goals,s.assists,s.yellow_cards,s.red_cards,s.updated_at";

    @Select("SELECT " + COLUMNS + " FROM player_season_stat s JOIN season_info se ON se.season_id=s.season_id " +
            "JOIN player_info p ON p.player_id=s.player_id WHERE s.stat_id=#{statId} LIMIT 1")
    PlayerSeasonStat findById(Long statId);

    @Select("SELECT " + COLUMNS + " FROM player_season_stat s JOIN season_info se ON se.season_id=s.season_id " +
            "JOIN player_info p ON p.player_id=s.player_id WHERE s.club_id=#{clubId} ORDER BY s.season_id DESC,s.goals DESC,s.stat_id")
    List<PlayerSeasonStat> findByClubId(Long clubId);

    @Select("SELECT COUNT(*) FROM player_season_stat WHERE season_id=#{seasonId} AND player_id=#{playerId} AND club_id=#{clubId} " +
            "AND (#{excludeId} IS NULL OR stat_id != #{excludeId})")
    int countDuplicate(@Param("seasonId") Long seasonId, @Param("playerId") Long playerId,
                       @Param("clubId") Long clubId, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM season_info WHERE season_id=#{seasonId}")
    int countSeason(Long seasonId);

    @Insert("""
            INSERT INTO player_season_stat (season_id,player_id,club_id,appearances,goals,assists)
            VALUES (#{seasonId},#{playerId},#{clubId},#{appearances},#{goals},#{assists})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "statId")
    int insert(PlayerSeasonStat stat);

    @Update("""
            UPDATE player_season_stat SET season_id=#{seasonId},player_id=#{playerId},appearances=#{appearances},
              goals=#{goals},assists=#{assists} WHERE stat_id=#{statId}
            """)
    int update(PlayerSeasonStat stat);
}
