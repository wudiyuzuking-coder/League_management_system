package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.dto.ClubRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubInfoMapper;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.vo.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class ClubInfoServiceImpl implements ClubInfoService {
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private final ClubInfoMapper clubMapper;

    @Override
    public ClubInfo getById(Long clubId) {
        ClubInfo club = clubMapper.findById(clubId);
        if (club == null) throw new BusinessException(HttpStatus.NOT_FOUND, "club not found");
        return club;
    }

    @Override
    public PageResponse<ClubInfo> list(ClubQueryRequest request) {
        validateStatusIfPresent(request.getStatus());
        long total = clubMapper.countPage(request.getName(), request.getStatus(), request.isWithoutLeader());
        long offset = (long) (request.getPage() - 1) * request.getSize();
        return new PageResponse<>(clubMapper.findPage(request.getName(), request.getStatus(), request.isWithoutLeader(),
                        offset, request.getSize()),
                total, request.getPage(), request.getSize());
    }

    @Override
    @Transactional
    public ClubInfo create(ClubRequest request) {
        assertNameAvailable(request.clubName(), null);
        ClubInfo club = fromRequest(new ClubInfo(), request);
        club.setClubStatus("ACTIVE");
        clubMapper.insert(club);
        return getById(club.getClubId());
    }

    @Override
    @Transactional
    public ClubInfo update(Long clubId, ClubRequest request) {
        ClubInfo club = getById(clubId);
        assertNameAvailable(request.clubName(), clubId);
        clubMapper.update(fromRequest(club, request));
        return getById(clubId);
    }

    @Override
    @Transactional
    public void updateStatus(Long clubId, String status) {
        getById(clubId);
        validateStatus(status);
        clubMapper.updateStatus(clubId, status);
    }

    private ClubInfo fromRequest(ClubInfo club, ClubRequest request) {
        club.setClubName(request.clubName().trim());
        club.setShortName(trimToNull(request.shortName()));
        club.setLogoUrl(trimToNull(request.logoUrl()));
        club.setHomeCity(request.homeCity().trim());
        club.setHomeAddress(trimToNull(request.homeAddress()));
        club.setHomeStadiumId(request.homeStadiumId());
        club.setDescription(trimToNull(request.description()));
        return club;
    }

    private void assertNameAvailable(String name, Long excludeId) {
        if (clubMapper.countByName(name.trim(), excludeId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "club name already exists");
        }
    }

    private void validateStatusIfPresent(String status) {
        if (status != null && !status.isBlank()) validateStatus(status);
    }

    private void validateStatus(String status) {
        if (!STATUSES.contains(status)) throw new BusinessException("invalid club status");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
