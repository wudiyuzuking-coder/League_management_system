package com.example.leagueticket.service;

import com.example.leagueticket.entity.StadiumInfo;
import com.example.leagueticket.dto.StadiumRequest;
import com.example.leagueticket.vo.StadiumCapacityResponse;
import java.util.List;
public interface StadiumInfoService {
    List<StadiumInfo> list(); List<StadiumInfo> search(String name,String city); StadiumInfo getById(Long id);
    StadiumInfo create(StadiumRequest request); StadiumInfo update(Long id,StadiumRequest request); StadiumInfo updateStatus(Long id,String status);
    StadiumCapacityResponse capacitySummary(Long id);
}
