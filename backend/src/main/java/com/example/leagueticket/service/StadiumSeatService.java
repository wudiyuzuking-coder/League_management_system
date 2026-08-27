package com.example.leagueticket.service;
import com.example.leagueticket.dto.SeatBatchRequest;
import com.example.leagueticket.dto.StadiumSeatRequest;
import com.example.leagueticket.entity.StadiumSeat;
import com.example.leagueticket.vo.SeatRowResponse;
import java.util.List;
public interface StadiumSeatService {List<StadiumSeat> list(Long zoneId);List<SeatRowResponse> layout(Long zoneId);StadiumSeat create(Long zoneId,StadiumSeatRequest request);StadiumSeat update(Long id,StadiumSeatRequest request);StadiumSeat updateStatus(Long id,String status);int batchCreate(Long zoneId,SeatBatchRequest request);}
