package com.example.leagueticket.service;

import com.example.leagueticket.dto.TicketQueryRequest;
import com.example.leagueticket.vo.ETicketResponse;
import com.example.leagueticket.vo.PageResponse;

public interface ETicketService {
    PageResponse<ETicketResponse> listOwned(Long userId,TicketQueryRequest query);
    ETicketResponse detailOwned(Long userId,Long ticketId);
}
