package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.entity.PlayerInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.PlayerInfoMapper;
import com.example.leagueticket.service.ClubDataScopeService;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.service.PlayerInfoService;
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
public class PlayerInfoServiceImpl implements PlayerInfoService {
    private static final Set<String> POSITIONS = Set.of("GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "TRANSFERRED");
    private final PlayerInfoMapper playerMapper;
    private final ClubInfoService clubService;
    private final ClubDataScopeService scopeService;

    @Override
    public PlayerInfo getById(Long playerId) {
        PlayerInfo player = playerMapper.findById(playerId);
        if (player == null) throw new BusinessException(HttpStatus.NOT_FOUND, "player not found");
        return player;
    }

    @Override
    public List<PlayerInfo> listByClub(Long clubId) {
        clubService.getById(clubId);
        return playerMapper.findByClubId(clubId);
    }

    @Override
    @Transactional
    public PlayerInfo create(Long clubId, PlayerRequest request) {
        clubService.getById(clubId);
        validatePosition(request.position());
        assertShirtAvailable(clubId, request.shirtNo(), null);
        PlayerInfo player = fromRequest(new PlayerInfo(), request);
        player.setClubId(clubId);
        player.setPlayerStatus("ACTIVE");
        playerMapper.insert(player);
        return getById(player.getPlayerId());
    }

    @Override
    @Transactional
    public PlayerInfo update(Long clubId, Long playerId, PlayerRequest request) {
        PlayerInfo player = getById(playerId);
        scopeService.requireSameClub(clubId, player.getClubId());
        validatePosition(request.position());
        assertShirtAvailable(clubId, request.shirtNo(), playerId);
        playerMapper.update(fromRequest(player, request));
        return getById(playerId);
    }

    @Override
    @Transactional
    public void updateStatus(Long clubId, Long playerId, String status) {
        PlayerInfo player = getById(playerId);
        scopeService.requireSameClub(clubId, player.getClubId());
        if (!STATUSES.contains(status)) throw new BusinessException("invalid player status");
        playerMapper.updateStatus(playerId, status);
    }

    private PlayerInfo fromRequest(PlayerInfo player, PlayerRequest request) {
        player.setPlayerName(request.playerName().trim());
        player.setShirtNo(request.shirtNo());
        player.setPosition(request.position());
        player.setNationality(request.nationality() == null || request.nationality().isBlank() ? null : request.nationality().trim());
        player.setBirthDate(request.birthDate());
        return player;
    }

    private void validatePosition(String position) {
        if (!POSITIONS.contains(position)) throw new BusinessException("invalid player position");
    }

    private void assertShirtAvailable(Long clubId, Integer shirtNo, Long excludeId) {
        if (playerMapper.countShirtNo(clubId, shirtNo, excludeId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "shirt number already exists in this club");
        }
    }
}
