package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.RoundInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoundInfoMapper {
    @Select("SELECT * FROM round_info WHERE season_id=#{seasonId} ORDER BY round_no") List<RoundInfo> findBySeasonId(Long seasonId);
    @Select("SELECT * FROM round_info WHERE round_id=#{id}") RoundInfo findById(Long id);
    @Select("SELECT COUNT(*) FROM round_info WHERE season_id=#{seasonId} AND round_no=#{roundNo} AND (#{excludeId} IS NULL OR round_id!=#{excludeId})")
    int countByRoundNo(@Param("seasonId") Long seasonId,@Param("roundNo") Integer roundNo,@Param("excludeId") Long excludeId);
    @Select("SELECT COUNT(*) FROM round_info WHERE season_id=#{seasonId} AND (start_date<#{startDate} OR end_date>#{endDate})")
    int countOutsideRange(@Param("seasonId") Long seasonId,@Param("startDate") java.time.LocalDate startDate,@Param("endDate") java.time.LocalDate endDate);
    @Insert("INSERT INTO round_info(season_id,round_no,round_name,start_date,end_date,round_status) VALUES(#{seasonId},#{roundNo},#{roundName},#{startDate},#{endDate},#{roundStatus})")
    @Options(useGeneratedKeys=true,keyProperty="roundId") int insert(RoundInfo round);
    @Update("UPDATE round_info SET round_no=#{roundNo},round_name=#{roundName},start_date=#{startDate},end_date=#{endDate} WHERE round_id=#{roundId}") int update(RoundInfo round);
    @Update("UPDATE round_info SET round_status=#{status} WHERE round_id=#{id}") int updateStatus(@Param("id") Long id,@Param("status") String status);
}
