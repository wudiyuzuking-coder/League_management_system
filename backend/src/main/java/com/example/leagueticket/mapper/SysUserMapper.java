package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper {

    String BASE_COLUMNS = """
            u.user_id, u.username, u.phone, u.password_hash,
            u.display_name AS real_name, u.role_id, r.role_code,
            u.club_id, u.user_status, u.last_login_at, u.created_at, u.updated_at
            """;

    String PAGE_SELECT = """
            <script>
            SELECT 
            """ + BASE_COLUMNS + """
             FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id
            <where>
              <if test='username != null and username != ""'>AND u.username LIKE CONCAT('%',#{username},'%')</if>
              <if test='phone != null and phone != ""'>AND u.phone LIKE CONCAT('%',#{phone},'%')</if>
              <if test='roleCode != null and roleCode != ""'>AND r.role_code=#{roleCode}</if>
              <if test='userStatus != null and userStatus != ""'>AND u.user_status=#{userStatus}</if>
            </where>
            ORDER BY u.user_id DESC LIMIT #{limit} OFFSET #{offset}
            </script>
            """;

    @Select("SELECT " + BASE_COLUMNS + " FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE u.username=#{username} LIMIT 1")
    SysUser findByUsername(String username);

    @Select("SELECT " + BASE_COLUMNS + " FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id WHERE u.user_id=#{userId} LIMIT 1")
    SysUser findById(Long userId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username=#{username} AND (#{excludeId} IS NULL OR user_id != #{excludeId})")
    int countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE phone=#{phone} AND (#{excludeId} IS NULL OR user_id != #{excludeId})")
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO sys_user
                (username, phone, password_hash, display_name, role_id, club_id, user_status)
            VALUES
                (#{username}, #{phone}, #{passwordHash}, #{realName}, #{roleId}, #{clubId}, #{userStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(SysUser user);

    @Update("UPDATE sys_user SET last_login_at=CURRENT_TIMESTAMP WHERE user_id=#{userId}")
    int updateLastLogin(Long userId);

    @Update("UPDATE sys_user SET display_name=#{realName}, phone=#{phone} WHERE user_id=#{userId}")
    int updateProfile(@Param("userId") Long userId, @Param("realName") String realName, @Param("phone") String phone);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash} WHERE user_id=#{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE sys_user
            SET display_name=#{realName}, phone=#{phone}, role_id=#{roleId}, club_id=#{clubId}, user_status=#{userStatus}
            WHERE user_id=#{userId}
            """)
    int updateByAdmin(SysUser user);

    @Update("UPDATE sys_user SET user_status=#{userStatus} WHERE user_id=#{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("userStatus") String userStatus);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash} WHERE password_hash='DEMO_PASSWORD_NOT_FOR_LOGIN'")
    int initializeDemoPasswords(String passwordHash);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON r.role_id=u.role_id
            <where>
              <if test='username != null and username != ""'>AND u.username LIKE CONCAT('%',#{username},'%')</if>
              <if test='phone != null and phone != ""'>AND u.phone LIKE CONCAT('%',#{phone},'%')</if>
              <if test='roleCode != null and roleCode != ""'>AND r.role_code=#{roleCode}</if>
              <if test='userStatus != null and userStatus != ""'>AND u.user_status=#{userStatus}</if>
            </where>
            </script>
            """)
    long countPage(@Param("username") String username, @Param("phone") String phone,
                   @Param("roleCode") String roleCode, @Param("userStatus") String userStatus);

    @Select(PAGE_SELECT)
    List<SysUser> findPage(@Param("username") String username, @Param("phone") String phone,
                           @Param("roleCode") String roleCode, @Param("userStatus") String userStatus,
                           @Param("offset") long offset, @Param("limit") int limit);
}
