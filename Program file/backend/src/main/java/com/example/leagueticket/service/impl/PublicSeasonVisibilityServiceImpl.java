package com.example.leagueticket.service.impl;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.PublicSeasonVisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class PublicSeasonVisibilityServiceImpl implements PublicSeasonVisibilityService {
    private final SeasonInfoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isPublicVisibleSeason(Long seasonId) {
        return seasonId != null && mapper.isPublicVisible(seasonId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requirePublicVisibleSeason(Long seasonId) {
        if (!isPublicVisibleSeason(seasonId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "public season not found");
        }
    }
}
