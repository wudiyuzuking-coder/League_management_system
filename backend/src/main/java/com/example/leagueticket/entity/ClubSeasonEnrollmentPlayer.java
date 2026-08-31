package com.example.leagueticket.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ClubSeasonEnrollmentPlayer {
    private Long enrollmentPlayerId;
    private Long enrollmentId;
    private Long playerId;
    private String lineupRole;
    private String playerNameSnapshot;
    private Integer shirtNoSnapshot;
    private String positionSnapshot;
    private LocalDate birthDateSnapshot;
}
