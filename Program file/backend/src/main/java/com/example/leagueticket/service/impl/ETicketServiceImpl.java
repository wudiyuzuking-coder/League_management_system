package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.TicketQueryRequest;
import com.example.leagueticket.entity.ETicket;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ETicketMapper;
import com.example.leagueticket.service.ETicketService;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class ETicketServiceImpl implements ETicketService {
    private static final Set<String> STATUSES=Set.of("UNUSED","USED","REFUNDED","VOID");
    private final ETicketMapper mapper;

    public PageResponse<ETicketResponse> listOwned(Long userId,TicketQueryRequest query){
        String status=normalize(query.getTicketStatus());
        long total=mapper.countOwned(userId,status);
        List<ETicketResponse> records=mapper.findOwnedPage(userId,status,(long)(query.getPage()-1)*query.getSize(),query.getSize()).stream().map(ETicketServiceImpl::response).toList();
        return new PageResponse<>(records,total,query.getPage(),query.getSize());
    }
    public ETicketResponse detailOwned(Long userId,Long ticketId){
        ETicket ticket=mapper.findOwnedDetail(ticketId,userId);
        if(ticket==null)throw new BusinessException(HttpStatus.NOT_FOUND,"ticket not found");
        return response(ticket);
    }
    private String normalize(String status){
        if(status==null||status.isBlank())return null;
        String value=status.trim().toUpperCase(Locale.ROOT);
        if(!STATUSES.contains(value))throw new BusinessException("invalid ticketStatus");
        return value;
    }
    static ETicketResponse response(ETicket t){return new ETicketResponse(t.getTicketId(),t.getTicketCode(),t.getOrderId(),t.getItemId(),t.getTicketStatus(),t.getIssuedAt(),t.getUsedAt(),t.getMatchId(),t.getHomeClubName(),t.getAwayClubName(),t.getMatchTime(),t.getStadiumName(),t.getZoneName(),t.getRowNo(),t.getSeatNo(),t.getTicketPrice());}
}
