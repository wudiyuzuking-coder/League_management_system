package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.entity.CoachInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.CoachInfoMapper;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.CoachInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class CoachInfoServiceImpl implements CoachInfoService {
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");
    private final CoachInfoMapper coachMapper;
    private final ClubInfoService clubService;
    private final ClubDataScopeService scopeService;

    @Override
    public CoachInfo getById(Long coachId) {
        CoachInfo coach = coachMapper.findById(coachId);
        if (coach == null) throw new BusinessException(HttpStatus.NOT_FOUND, "coach not found");
        return coach;
    }

    @Override
    public List<CoachInfo> listByClub(Long clubId) {
        clubService.getById(clubId);
        return coachMapper.findByClubId(clubId);
    }

    @Override
    @Transactional
    public CoachInfo create(Long clubId, CoachRequest request) {
        clubService.getById(clubId);
        CoachInfo coach = fromRequest(new CoachInfo(), request);
        coach.setClubId(clubId);
        coach.setCoachStatus("ACTIVE");
        coachMapper.insert(coach);
        return getById(coach.getCoachId());
    }

    @Override
    @Transactional
    public CoachInfo update(Long clubId, Long coachId, CoachRequest request) {
        CoachInfo coach = getById(coachId);
        scopeService.requireSameClub(clubId, coach.getClubId());
        coachMapper.update(fromRequest(coach, request));
        return getById(coachId);
    }

    @Override
    @Transactional
    public void updateStatus(Long clubId, Long coachId, String status) {
        CoachInfo coach = getById(coachId);
        scopeService.requireSameClub(clubId, coach.getClubId());
        if (!STATUSES.contains(status)) throw new BusinessException("invalid coach status");
        coachMapper.updateStatus(coachId, status);
    }

    private CoachInfo fromRequest(CoachInfo coach, CoachRequest request) {
        coach.setCoachName(request.coachName().trim());
        coach.setTitle(request.title().trim());
        coach.setNationality(request.nationality() == null || request.nationality().isBlank() ? null : request.nationality().trim());
        coach.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        return coach;
    }
}
