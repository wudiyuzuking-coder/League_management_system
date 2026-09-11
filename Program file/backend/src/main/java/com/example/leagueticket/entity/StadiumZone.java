package com.example.leagueticket.entity;

import lombok.Data;

@Data
public class StadiumZone {
    private Long stadiumZoneId;
    private Long stadiumId;
    private String zoneCode;
    private String zoneName;
    private String zoneDirection;
    private String ticketType;
    private Integer sortNo;
    private String zoneStatus;
    private String description;
}
