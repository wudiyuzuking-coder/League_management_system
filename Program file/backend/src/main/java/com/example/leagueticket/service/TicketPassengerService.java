package com.example.leagueticket.service;

import com.example.leagueticket.dto.PrefilledPassengerRequest;
import com.example.leagueticket.entity.UserPrefilledPassenger;
import com.example.leagueticket.vo.PrefilledPassengerResponse;
import java.util.List;

public interface TicketPassengerService {
    List<PrefilledPassengerResponse> list(Long userId);
    PrefilledPassengerResponse add(Long userId,PrefilledPassengerRequest request);
    void delete(Long userId,Long id);
    List<UserPrefilledPassenger> requireForOrder(Long userId,List<Long> ids,int ticketCount);
}
