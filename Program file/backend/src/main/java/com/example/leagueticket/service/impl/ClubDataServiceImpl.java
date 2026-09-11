package com.example.leagueticket.service.impl;
import com.example.leagueticket.mapper.ClubDataMapper;
import com.example.leagueticket.service.ClubDataService;
import com.example.leagueticket.vo.ClubDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Profile("dev") @RequiredArgsConstructor @Transactional(readOnly=true)
public class ClubDataServiceImpl implements ClubDataService {private final ClubDataMapper mapper;public ClubDataResponse get(Long clubId){return new ClubDataResponse(mapper.revenue(clubId),mapper.performance(clubId),mapper.rankings());}}
