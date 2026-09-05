package com.example.leagueticket.entity;

import lombok.Data;

@Data
public class StadiumInfo {
    private Long stadiumId;
    private String stadiumName;
    private String city;
    private String address;
    private Integer capacity;
    private String layoutDesc;
    private String stadiumStatus;
}
