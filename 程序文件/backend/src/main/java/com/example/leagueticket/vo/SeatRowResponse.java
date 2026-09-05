package com.example.leagueticket.vo;
import com.example.leagueticket.entity.StadiumSeat;
import java.util.List;
public record SeatRowResponse(int rowNo,String rowLabel,List<StadiumSeat> seats) {}
