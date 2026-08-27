package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StadiumSeat {
    private Long stadiumSeatId;
    private Long stadiumId;
    private Long stadiumZoneId;
    private Integer rowNo;
    private String rowLabel;
    private Integer seatNo;
    private String seatLabel;
    private BigDecimal centerDistance;
    private String seatStatus;
}
