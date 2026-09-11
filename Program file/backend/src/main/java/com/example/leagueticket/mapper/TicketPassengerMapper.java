package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.TicketPassengerIdentity;
import com.example.leagueticket.entity.UserPrefilledPassenger;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TicketPassengerMapper {
    @Insert("INSERT IGNORE INTO ticket_passenger_identity(id_card_no) VALUES(#{idCardNo})") int ensureIdentity(String idCardNo);
    @Select("SELECT * FROM ticket_passenger_identity WHERE id_card_no=#{idCardNo} FOR UPDATE") TicketPassengerIdentity findIdentityForUpdate(String idCardNo);
    @Select("SELECT * FROM ticket_passenger_identity WHERE passenger_identity_id=#{id} FOR UPDATE") TicketPassengerIdentity findIdentityByIdForUpdate(Long id);
    @Select("SELECT COUNT(*) FROM user_prefilled_passenger WHERE user_id=#{userId}") int countByUser(Long userId);
    @Select("SELECT COUNT(*) FROM user_prefilled_passenger WHERE passenger_identity_id=#{identityId}") int countByIdentity(Long identityId);
    @Select("SELECT up.*,pi.id_card_no FROM user_prefilled_passenger up JOIN ticket_passenger_identity pi ON pi.passenger_identity_id=up.passenger_identity_id WHERE up.user_id=#{userId} ORDER BY up.prefilled_passenger_id") List<UserPrefilledPassenger> findByUser(Long userId);
    @Select("SELECT up.*,pi.id_card_no FROM user_prefilled_passenger up JOIN ticket_passenger_identity pi ON pi.passenger_identity_id=up.passenger_identity_id WHERE up.prefilled_passenger_id=#{id} AND up.user_id=#{userId} FOR UPDATE") UserPrefilledPassenger findOwnedForUpdate(@Param("id")Long id,@Param("userId")Long userId);
    @Select("SELECT COUNT(*) FROM user_prefilled_passenger WHERE user_id=#{userId} AND passenger_identity_id=#{identityId}") int countUserIdentity(@Param("userId")Long userId,@Param("identityId")Long identityId);
    @Insert("INSERT INTO user_prefilled_passenger(user_id,passenger_identity_id,passenger_name) VALUES(#{userId},#{passengerIdentityId},#{passengerName})") @Options(useGeneratedKeys=true,keyProperty="prefilledPassengerId") int insert(UserPrefilledPassenger value);
    @Delete("DELETE FROM user_prefilled_passenger WHERE prefilled_passenger_id=#{id} AND user_id=#{userId}") int deleteOwned(@Param("id")Long id,@Param("userId")Long userId);
}
