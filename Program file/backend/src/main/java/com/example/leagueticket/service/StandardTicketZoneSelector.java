package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchTicketZone;

public interface StandardTicketZoneSelector {
    MatchTicketZone select(Long matchId, String ticketType, int ticketCount, boolean lockZones);
}
