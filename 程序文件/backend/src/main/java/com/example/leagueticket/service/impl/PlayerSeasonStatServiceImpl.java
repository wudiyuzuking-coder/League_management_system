package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PlayerSeasonStatRequest;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.entity.PlayerSeasonStat;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.PlayerSeasonStatMapper;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.PlayerInfoService;
import com.example.leagueticket.service.PlayerSeasonStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class PlayerSeasonStatServiceImpl implements PlayerSeasonStatService {
    private final PlayerSeasonStatMapper statMapper;
    private final PlayerInfoService playerService;
    private final ClubInfoService clubService;
    private final ClubDataScopeService scopeService;

    @Override
    public List<PlayerSeasonStat> listByClub(Long clubId) {
        clubService.getById(clubId);
        return statMapper.findByClubId(clubId);
    }

    @Override
    @Transactional
    public PlayerSeasonStat create(Long clubId, PlayerSeasonStatRequest request) {
        clubService.getById(clubId);
        validateRelations(clubId, request, null);
        PlayerSeasonStat stat = fromRequest(new PlayerSeasonStat(), clubId, request);
        statMapper.insert(stat);
        return getById(stat.getStatId());
    }

    @Override
    @Transactional
    public PlayerSeasonStat update(Long clubId, Long statId, PlayerSeasonStatRequest request) {
        PlayerSeasonStat stat = getById(statId);
        scopeService.requireSameClub(clubId, stat.getClubId());
        validateRelations(clubId, request, statId);
        statMapper.update(fromRequest(stat, clubId, request));
        return getById(statId);
    }

    @Override
    public PlayerSeasonStat getById(Long statId) {
        PlayerSeasonStat stat = statMapper.findById(statId);
        if (stat == null) throw new BusinessException(HttpStatus.NOT_FOUND, "player season stat not found");
        return stat;
    }

    private void validateRelations(Long clubId, PlayerSeasonStatRequest request, Long excludeId) {
        if (statMapper.countSeason(request.seasonId()) == 0) throw new BusinessException(HttpStatus.NOT_FOUND, "season not found");
        PlayerInfo player = playerService.getById(request.playerId());
        scopeService.requireSameClub(clubId, player.getClubId());
        if (statMapper.countDuplicate(request.seasonId(), request.playerId(), clubId, excludeId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "player season stat already exists");
        }
    }

    private PlayerSeasonStat fromRequest(PlayerSeasonStat stat, Long clubId, PlayerSeasonStatRequest request) {
        stat.setSeasonId(request.seasonId());
        stat.setPlayerId(request.playerId());
        stat.setClubId(clubId);
        stat.setAppearances(request.appearances());
        stat.setGoals(request.goals());
        stat.setAssists(request.assists());
        return stat;
    }
}
