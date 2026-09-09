package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchSeatInventory;
import com.example.leagueticket.vo.InventoryRowResponse;
import com.example.leagueticket.vo.TicketZoneAvailabilityResponse;
import java.util.List;

public interface MatchSeatInventoryService {
    int generate(Long matchZoneId);
    MatchSeatInventory updateStatus(Long inventoryId,String status);
    TicketZoneAvailabilityResponse availability(Long matchZoneId);
    List<InventoryRowResponse> layout(Long matchZoneId);
}
