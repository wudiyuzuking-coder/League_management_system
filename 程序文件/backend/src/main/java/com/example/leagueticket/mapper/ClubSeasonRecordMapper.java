package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.ClubSeasonRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ClubSeasonRecordMapper {
    @Select("SELECT * FROM club_season_record WHERE record_id=#{id}") ClubSeasonRecord findById(Long id);
    @Select("""
        SELECT r.*,c.club_name,c.logo_url FROM club_season_record r
        JOIN club_info c ON c.club_id=r.club_id WHERE r.season_id=#{seasonId}
        ORDER BY r.points DESC,(CAST(r.goals_for AS SIGNED)-CAST(r.goals_against AS SIGNED)) DESC,r.goals_for DESC,r.club_id ASC
        """) List<RecordRow> findStandings(Long seasonId);
    @Insert("""
        INSERT IGNORE INTO club_season_record(season_id,club_id,played,wins,draws,losses,goals_for,goals_against,points,ranking)
        SELECT #{seasonId},club_id,0,0,0,0,0,0,0,NULL FROM club_info WHERE club_status='ACTIVE'
        """) int initializeActiveClubs(Long seasonId);
    @Update("UPDATE club_season_record SET played=#{played},wins=#{wins},draws=#{draws},losses=#{losses},goals_for=#{goalsFor},goals_against=#{goalsAgainst},points=#{points},ranking=NULL WHERE record_id=#{recordId}") int update(ClubSeasonRecord record);
    @Insert("INSERT IGNORE INTO club_season_record(season_id,club_id,played,wins,draws,losses,goals_for,goals_against,points,ranking) VALUES(#{seasonId},#{clubId},0,0,0,0,0,0,0,NULL)")
    int ensureRecord(@Param("seasonId") Long seasonId,@Param("clubId") Long clubId);
    @Update("UPDATE club_season_record SET played=0,wins=0,draws=0,losses=0,goals_for=0,goals_against=0,points=0,ranking=NULL WHERE season_id=#{seasonId}")
    int resetSeason(Long seasonId);
    @Select("SELECT * FROM club_season_record WHERE season_id=#{seasonId} AND club_id=#{clubId}")
    ClubSeasonRecord findBySeasonAndClub(@Param("seasonId") Long seasonId,@Param("clubId") Long clubId);

    class RecordRow extends ClubSeasonRecord {
        private String clubName;
        private String logoUrl;
        public String getClubName(){return clubName;}
        public void setClubName(String clubName){this.clubName=clubName;}
        public String getLogoUrl(){return logoUrl;}
        public void setLogoUrl(String logoUrl){this.logoUrl=logoUrl;}
    }
}
