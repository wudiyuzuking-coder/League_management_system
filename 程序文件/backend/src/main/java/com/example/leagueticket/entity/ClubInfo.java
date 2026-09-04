package com.example.leagueticket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClubInfo {
    private Long clubId;
    private String clubName;
    private String shortName;
    private String logoUrl;
    private String homeCity;
    private String homeAddress;
    private Long homeStadiumId;
    private String description;
    private String clubStatus;
    private String leaderName;
    private String leaderPhone;
    private String leaderNickname;
    private String leaderStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
