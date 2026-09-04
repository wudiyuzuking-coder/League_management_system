package com.example.leagueticket.service;
import com.example.leagueticket.dto.StadiumZoneRequest;
import com.example.leagueticket.entity.StadiumZone;
import java.util.List;
public interface StadiumZoneService {List<StadiumZone> list(Long stadiumId);StadiumZone getById(Long id);StadiumZone create(Long stadiumId,StadiumZoneRequest request);StadiumZone update(Long id,StadiumZoneRequest request);StadiumZone updateStatus(Long id,String status);}
