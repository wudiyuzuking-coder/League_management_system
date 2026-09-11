package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PlayerRequest;
import com.example.leagueticket.dto.PlayerAdjustRequest;
import com.example.leagueticket.dto.PlayerReturnRequest;
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
    private static final Set<String> LINEUP_ROLES = Set.of("STARTER", "SUBSTITUTE");
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
        validateLineup(request.lineupRole());
        assertShirtAvailable(clubId, request.shirtNo(), null);
        assertRosterCapacity(clubId, request.lineupRole(), request.position(), null);
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
    public PlayerInfo adjust(Long clubId, Long playerId, PlayerAdjustRequest request) {
        PlayerInfo player=getById(playerId);
        scopeService.requireSameClub(clubId,player.getClubId());
        if(!"ACTIVE".equals(player.getPlayerStatus()))throw new BusinessException("离队球员请先归队");
        validatePosition(request.position());validateLineup(request.lineupRole());
        assertShirtAvailable(clubId,request.shirtNo(),playerId);
        assertRosterCapacity(clubId,request.lineupRole(),request.position(),playerId);
        player.setShirtNo(request.shirtNo());player.setPosition(request.position());player.setLineupRole(request.lineupRole());
        playerMapper.adjust(player);return getById(playerId);
    }

    @Override
    @Transactional
    public PlayerInfo returnToTeam(Long clubId,Long playerId,PlayerReturnRequest request){
        PlayerInfo player=getById(playerId);scopeService.requireSameClub(clubId,player.getClubId());
        if(!"TRANSFERRED".equals(player.getPlayerStatus()))throw new BusinessException("只有离队球员可以归队");
        validatePosition(request.position());validateLineup(request.lineupRole());assertShirtAvailable(clubId,request.shirtNo(),playerId);
        assertRosterCapacity(clubId,request.lineupRole(),request.position(),playerId);
        player.setShirtNo(request.shirtNo());player.setPosition(request.position());player.setLineupRole(request.lineupRole());
        playerMapper.returnToTeam(player);return getById(playerId);
    }

    @Override
    @Transactional
    public void updateStatus(Long clubId, Long playerId, String status) {
        PlayerInfo player = getById(playerId);
        scopeService.requireSameClub(clubId, player.getClubId());
        if (!STATUSES.contains(status)) throw new BusinessException("invalid player status");
        if("TRANSFERRED".equals(status)){playerMapper.markLeft(playerId);return;}
        playerMapper.updateStatus(playerId, status);
    }

    @Override @Transactional
    public void cleanup(Long clubId,Long playerId){PlayerInfo player=getById(playerId);scopeService.requireSameClub(clubId,player.getClubId());
        if(!"TRANSFERRED".equals(player.getPlayerStatus()))throw new BusinessException("只有离队球员可以清理");
        if(playerMapper.countEnrollmentSnapshots(playerId)>0)throw new BusinessException(HttpStatus.CONFLICT,"球员已有报名阵容快照，不能清理");
        if(playerMapper.deleteLeft(playerId)!=1)throw new BusinessException(HttpStatus.CONFLICT,"球员清理失败");}

    private PlayerInfo fromRequest(PlayerInfo player, PlayerRequest request) {
        player.setPlayerName(request.playerName().trim());
        player.setShirtNo(request.shirtNo());
        player.setPosition(request.position());
        player.setNationality(request.nationality() == null || request.nationality().isBlank() ? null : request.nationality().trim());
        player.setBirthDate(request.birthDate());
        player.setBirthYear(request.birthYear());
        player.setLineupRole(request.lineupRole());
        return player;
    }

    private void validateLineup(String role){if(!LINEUP_ROLES.contains(role))throw new BusinessException("请选择首发或替补");}
    private void assertRosterCapacity(Long clubId,String role,String position,Long excludeId){
        if("STARTER".equals(role)&&playerMapper.countActiveRole(clubId,"STARTER",excludeId)>=11)throw new BusinessException("首发球员最多11名");
        if("SUBSTITUTE".equals(role)&&playerMapper.countActiveRole(clubId,"SUBSTITUTE",excludeId)>=7)throw new BusinessException("替补球员最多7名");
        if("STARTER".equals(role)&&"GOALKEEPER".equals(position)&&playerMapper.countStartingGoalkeepers(clubId,excludeId)>=1)throw new BusinessException("首发门将只能有1名");
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
