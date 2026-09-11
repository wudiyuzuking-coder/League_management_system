package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.CoachRequest;
import com.example.leagueticket.dto.CoachAdjustRequest;
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
    private static final Set<String> TITLES = Set.of("HEAD_COACH", "ASSISTANT_COACH");
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
        validateTitle(request.title());assertCapacity(clubId,request.title(),null);
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

    @Override @Transactional
    public CoachInfo adjust(Long clubId,Long coachId,CoachAdjustRequest request){CoachInfo coach=getById(coachId);scopeService.requireSameClub(clubId,coach.getClubId());
        if(!"ACTIVE".equals(coach.getCoachStatus()))throw new BusinessException("离队教练请先归队");validateTitle(request.title());assertCapacity(clubId,request.title(),coachId);
        coach.setTitle(request.title());coachMapper.adjust(coach);return getById(coachId);}

    @Override
    @Transactional
    public void updateStatus(Long clubId, Long coachId, String status) {
        CoachInfo coach = getById(coachId);
        scopeService.requireSameClub(clubId, coach.getClubId());
        if (!STATUSES.contains(status)) throw new BusinessException("invalid coach status");
        if("ACTIVE".equals(status)){validateTitle(coach.getTitle());assertCapacity(clubId,coach.getTitle(),coachId);}
        coachMapper.updateStatus(coachId, status);
    }

    @Override @Transactional public void cleanup(Long clubId,Long coachId){CoachInfo coach=getById(coachId);scopeService.requireSameClub(clubId,coach.getClubId());
        if(!"INACTIVE".equals(coach.getCoachStatus()))throw new BusinessException("只有离队教练可以清理");
        if(coachMapper.countEnrollmentSnapshots(coachId)>0)throw new BusinessException(HttpStatus.CONFLICT,"教练已有报名阵容快照，不能清理");
        if(coachMapper.deleteLeft(coachId)!=1)throw new BusinessException(HttpStatus.CONFLICT,"教练清理失败");}

    private CoachInfo fromRequest(CoachInfo coach, CoachRequest request) {
        coach.setCoachName(request.coachName().trim());
        coach.setTitle(request.title().trim());
        coach.setNationality(request.nationality() == null || request.nationality().isBlank() ? null : request.nationality().trim());
        coach.setBirthYear(request.birthYear());
        coach.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        return coach;
    }

    private void validateTitle(String title){if(!TITLES.contains(title))throw new BusinessException("请选择主教练或副教练");}
    private void assertCapacity(Long clubId,String title,Long excludeId){int max="HEAD_COACH".equals(title)?1:2;if(coachMapper.countActiveTitle(clubId,title,excludeId)>=max)throw new BusinessException("HEAD_COACH".equals(title)?"现役主教练只能有1名":"现役副教练最多2名");}
}
