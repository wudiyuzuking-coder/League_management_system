package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.ClubInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ClubInfoMapper {
    String WITH_LEADER = """
            SELECT c.*, u.display_name AS leader_name, u.phone AS leader_phone,
                   u.username AS leader_nickname, u.user_status AS leader_status
            FROM club_info c
            LEFT JOIN sys_user u ON u.club_id=c.club_id
              AND u.role_id=(SELECT role_id FROM sys_role WHERE role_code='CLUB' LIMIT 1)
            """;

    @Select(WITH_LEADER + " WHERE c.club_id=#{clubId} LIMIT 1")
    ClubInfo findById(Long clubId);

    @Select(WITH_LEADER + " WHERE c.club_id=#{clubId} AND c.club_status='ACTIVE' LIMIT 1")
    ClubInfo findActiveById(Long clubId);

    @Select("SELECT * FROM club_info WHERE club_id=#{clubId} LIMIT 1 FOR UPDATE")
    ClubInfo findByIdForUpdate(Long clubId);

    @Select("SELECT COUNT(*) FROM club_info WHERE club_name=#{clubName} AND (#{excludeId} IS NULL OR club_id != #{excludeId})")
    int countByName(@Param("clubName") String clubName, @Param("excludeId") Long excludeId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM club_info c
            LEFT JOIN sys_user u ON u.club_id=c.club_id
              AND u.role_id=(SELECT role_id FROM sys_role WHERE role_code='CLUB' LIMIT 1)
            <where>
              <if test='name != null and name != ""'>AND c.club_name LIKE CONCAT('%',#{name},'%')</if>
              <if test='status != null and status != ""'>AND c.club_status=#{status}</if>
              <if test='withoutLeader'>AND u.user_id IS NULL</if>
            </where>
            </script>
            """)
    long countPage(@Param("name") String name, @Param("status") String status,
                   @Param("withoutLeader") boolean withoutLeader);

    @Select("""
            <script>
            """ + WITH_LEADER + """
            <where>
              <if test='name != null and name != ""'>AND c.club_name LIKE CONCAT('%',#{name},'%')</if>
              <if test='status != null and status != ""'>AND c.club_status=#{status}</if>
              <if test='withoutLeader'>AND u.user_id IS NULL</if>
            </where>
            ORDER BY c.club_id DESC LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ClubInfo> findPage(@Param("name") String name, @Param("status") String status,
                            @Param("withoutLeader") boolean withoutLeader,
                            @Param("offset") long offset, @Param("limit") int limit);

    @Insert("""
            INSERT INTO club_info
              (club_name,short_name,logo_url,home_city,home_address,home_stadium_id,description,club_status)
            VALUES
              (#{clubName},#{shortName},#{logoUrl},#{homeCity},#{homeAddress},#{homeStadiumId},#{description},#{clubStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "clubId")
    int insert(ClubInfo club);

    @Update("""
            UPDATE club_info SET club_name=#{clubName},short_name=#{shortName},logo_url=#{logoUrl},
              home_city=#{homeCity},home_address=#{homeAddress},home_stadium_id=#{homeStadiumId},description=#{description}
            WHERE club_id=#{clubId}
            """)
    int update(ClubInfo club);

    @Update("UPDATE club_info SET club_status=#{status} WHERE club_id=#{clubId}")
    int updateStatus(@Param("clubId") Long clubId, @Param("status") String status);
}
