package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlayerInfo {
    private Long playerId;
    private Long clubId;
    private String playerName;
    private Integer shirtNo;
    private String position;
    private String nationality;
    private LocalDate birthDate;
    private String playerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
