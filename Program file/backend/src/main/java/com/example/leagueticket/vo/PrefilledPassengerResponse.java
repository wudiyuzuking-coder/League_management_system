package com.example.leagueticket.vo;

import com.example.leagueticket.entity.UserPrefilledPassenger;
import java.time.LocalDateTime;

public record PrefilledPassengerResponse(Long prefilledPassengerId,String passengerName,String idCardNo,
                                         LocalDateTime createdAt) {
    public static PrefilledPassengerResponse from(UserPrefilledPassenger value){return new PrefilledPassengerResponse(value.getPrefilledPassengerId(),value.getPassengerName(),value.getIdCardNo(),value.getCreatedAt());}
}
