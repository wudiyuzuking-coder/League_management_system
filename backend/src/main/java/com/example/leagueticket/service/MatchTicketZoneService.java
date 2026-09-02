package com.example.leagueticket.service;

import com.example.leagueticket.dto.MatchTicketZoneRequest;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.vo.MatchTicketZoneResponse;
import com.example.leagueticket.vo.UserMatchTicketZoneResponse;
import java.util.List;

public interface MatchTicketZoneService {
    List<MatchTicketZoneResponse> list(Long matchId);
    MatchTicketZoneResponse detail(Long id);
    List<UserMatchTicketZoneResponse> listPublic(Long matchId);
    UserMatchTicketZoneResponse detailPublic(Long id);
    MatchTicketZone create(Long matchId,Long creatorId,MatchTicketZoneRequest request);
    MatchTicketZone update(Long id,MatchTicketZoneRequest request);
    MatchTicketZone updateStatus(Long id,String status);
    MatchTicketZone getEntity(Long id);
}
