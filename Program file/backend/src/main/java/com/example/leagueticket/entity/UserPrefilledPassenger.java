package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserPrefilledPassenger {
    private Long prefilledPassengerId;
    private Long userId;
    private Long passengerIdentityId;
    private String passengerName;
    private String idCardNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
