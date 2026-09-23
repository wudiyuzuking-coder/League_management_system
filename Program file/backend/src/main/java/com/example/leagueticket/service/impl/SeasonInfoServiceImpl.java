package com.example.leagueticket.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.leagueticket.dto.SeasonRequest;
import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.RoundInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.DoubleRoundRobinSchedulePlanner;
import com.example.leagueticket.service.PublicSeasonVisibilityService;
import com.example.leagueticket.service.SeasonInfoService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.PublicSeasonResponse;

import lombok.RequiredArgsConstructor;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SeasonInfoServiceImpl implements SeasonInfoService {
    private final SeasonInfoMapper mapper;
    private final RoundInfoMapper roundMapper;
    private final SystemTimeService timeService;
    private final DoubleRoundRobinSchedulePlanner planner;
    private final PublicSeasonVisibilityService publicVisibility;

    public List<SeasonInfo> list() {
        return mapper.findAll();
    }

    public List<PublicSeasonResponse> listPublic() {
        return mapper.findPublic();
    }

    public PublicSeasonResponse getPublicById(Long id) {
        publicVisibility.requirePublicVisibleSeason(id);
        PublicSeasonResponse value = mapper.findPublicById(id);
        if (value == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "public season not found");
        return value;
    }

    public SeasonInfo getById(Long id) {
        SeasonInfo value = mapper.findById(id);
        if (value == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        return value;
    }

    @Transactional
    public SeasonInfo create(SeasonRequest request) {
        validate(request);
        SeasonInfo s = derive(new SeasonInfo(), request);
        s.setSeasonStatus(SeasonStatus.DRAFT.name());
        mapper.insert(s);
        return getById(s.getSeasonId());
    }

    @Transactional
    public SeasonInfo update(Long id, SeasonRequest request) {
        SeasonInfo s = getById(id);
        if (!SeasonStatus.DRAFT.matches(s.getSeasonStatus()))
            throw new BusinessException(HttpStatus.CONFLICT, "仅草稿阶段允许调整赛季");
        validate(request);
        if (!roundMapper.findBySeasonId(id).isEmpty())
            throw new BusinessException(HttpStatus.CONFLICT, "a scheduled season cannot be edited");
        mapper.update(derive(s, request));
        return getById(id);
    }

    private void validate(SeasonRequest r) {
        if (r.seasonName() == null || r.seasonName().trim().isEmpty())
            throw new BusinessException("赛季名称不能为空");
        if (r.startDate() == null)
            throw new BusinessException("赛季开始日期不能为空");
        if (r.maxClubs() == null)
            throw new BusinessException("参赛队伍上限不能为空");
        if (r.maxClubs() < 2 || r.maxClubs() > 20)
            throw new BusinessException("maxClubs must be between 2 and 20");
        if (r.endDate() != null && !r.startDate().isBefore(r.endDate()))
            throw new BusinessException("赛季开始日期必须早于赛季结束日期");
        if ((r.registrationStartTime() == null) != (r.registrationDeadline() == null))
            throw new BusinessException("报名开始时间和报名截止时间必须同时提供");
        if (r.registrationStartTime() != null
                && !r.registrationStartTime().isBefore(r.registrationDeadline()))
            throw new BusinessException("报名开始时间必须早于报名截止时间");
        if (r.registrationDeadline() != null
                && !r.registrationDeadline().isBefore(r.startDate().atStartOfDay()))
            throw new BusinessException("报名截止时间必须早于赛季开始日期");
        LocalDate minimum = timeService.now().plusMonths(1).toLocalDate();
        if (r.startDate().isBefore(minimum))
            throw new BusinessException("赛季开始日期必须不早于系统时间一个月后");
    }

    private SeasonInfo derive(SeasonInfo s, SeasonRequest r) {
        LocalDateTime now = timeService.now();
        LocalDateTime registrationStart = nextTwenty(now);
        LocalDateTime deadline = r.startDate().minusDays(15).atTime(19, 59);
        if (!deadline.isAfter(registrationStart))
            throw new BusinessException("自动报名窗口不足，请选择更晚的赛季开始日期");
        s.setSeasonName(r.seasonName().trim());
        s.setStartDate(r.startDate());
        s.setEndDate(planner.expectedEndDate(r.maxClubs(), r.startDate()));
        s.setRegistrationStartTime(registrationStart);
        s.setRegistrationDeadline(deadline);
        s.setTicketSaleStartTime(null);
        s.setMaxClubs(r.maxClubs());
        s.setDescription(null);
        return s;
    }

    private LocalDateTime nextTwenty(LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atTime(20, 0);
        return now.isBefore(today) ? today : today.plusDays(1);
    }
}
