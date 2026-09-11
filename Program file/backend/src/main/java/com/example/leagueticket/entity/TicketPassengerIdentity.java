package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketPassengerIdentity {
    private Long passengerIdentityId;
    private String idCardNo;
    private LocalDateTime createdAt;
}
