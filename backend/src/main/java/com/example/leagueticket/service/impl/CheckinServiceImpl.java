package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.CheckinService;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {
    private static final Set<String> RESULTS=Set.of("SUCCESS","CODE_NOT_FOUND","WRONG_MATCH",
            "ORDER_INVALID","TICKET_USED","TICKET_REFUNDED","TICKET_VOID");
    private static final Set<String> MATCH_STATUSES=Set.of("DRAFT","PUBLISHED","IN_PROGRESS","FINISHED","CANCELLED");
    private static final Set<String> OPEN_MATCH_STATUSES=Set.of("PUBLISHED","IN_PROGRESS");
    private final MatchInfoMapper matchMapper;
    private final ETicketMapper ticketMapper;
    private final TicketOrderMapper orderMapper;
    private final CheckinRecordMapper recordMapper;

    @Override
    public List<CheckerMatchResponse> matches(AuthenticatedUser user,CheckerMatchQueryRequest query){
        Long clubScope=clubScope(user);String status=matchStatus(query.getMatchStatus());
        if(query.getStartDate()!=null&&query.getEndDate()!=null&&query.getEndDate().isBefore(query.getStartDate()))
            throw new BusinessException("endDate must not be before startDate");
        LocalDateTime start=query.getStartDate()==null?null:query.getStartDate().atStartOfDay();
        LocalDateTime end=query.getEndDate()==null?null:query.getEndDate().plusDays(1).atStartOfDay();
        return matchMapper.findCheckerMatches(clubScope,status,start,end).stream().map(this::matchResponse).toList();
    }

    @Override @Transactional
    public CheckinResponse checkin(AuthenticatedUser user,Long matchId,CheckinRequest request){
        MatchInfo match=matchMapper.findById(matchId);
        if(match==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");
        requireMatchScope(user,match);
        if(!OPEN_MATCH_STATUSES.contains(match.getMatchStatus()))
            throw new BusinessException(HttpStatus.CONFLICT,"check-in is allowed only for PUBLISHED or IN_PROGRESS matches");
        String code=request.ticketCode().trim();ETicket found=ticketMapper.findByCode(code);
        if(found==null)return record(user,match,null,code,"CODE_NOT_FOUND","ticket code was not found",LocalDateTime.now());
        if(!matchId.equals(found.getMatchId()))
            return record(user,match,found,code,"WRONG_MATCH","ticket belongs to another match",LocalDateTime.now());

        TicketOrder order=orderMapper.findByIdForUpdate(found.getOrderId());
        ETicket ticket=ticketMapper.findByIdForUpdate(found.getTicketId());
        if(order==null||ticket==null)
            return record(user,match,found,code,"ORDER_INVALID","ticket order is unavailable",LocalDateTime.now());
        if("REFUND_PENDING".equals(order.getOrderStatus()))
            return record(user,match,found,code,"ORDER_INVALID","order is awaiting refund review",LocalDateTime.now());
        String stateResult=ticketResult(ticket.getTicketStatus());
        if(stateResult!=null)return record(user,match,found,code,stateResult,stateMessage(ticket),LocalDateTime.now());
        if(!"PAID".equals(order.getOrderStatus()))
            return record(user,match,found,code,"ORDER_INVALID","order status is "+order.getOrderStatus(),LocalDateTime.now());
        LocalDateTime now=LocalDateTime.now();
        if(ticketMapper.markUsed(ticket.getTicketId(),now)!=1)
            return record(user,match,found,code,"TICKET_USED","ticket has already been used",LocalDateTime.now());
        return record(user,match,found,code,"SUCCESS","admission allowed",now);
    }

    @Override
    public PageResponse<CheckinRecordResponse> ownRecords(AuthenticatedUser user,CheckinQueryRequest query){
        if(!"CHECKER".equals(user.roleCode()))throw new BusinessException(HttpStatus.FORBIDDEN,"checker role is required");
        requireClub(user);return page(user.userId(),query);
    }
    @Override public PageResponse<CheckinRecordResponse> adminRecords(CheckinQueryRequest query){return page(query.getCheckerId(),query);}
    @Override public CheckinRecordResponse adminDetail(Long id){CheckinRecord r=recordMapper.findDetail(id);if(r==null)throw new BusinessException(HttpStatus.NOT_FOUND,"check-in record not found");return response(r);}

    private PageResponse<CheckinRecordResponse> page(Long checkerId,CheckinQueryRequest q){
        validateRange(q);String result=result(q.getCheckResult());long total=recordMapper.count(checkerId,q.getMatchId(),result,q.getStartTime(),q.getEndTime());
        List<CheckinRecordResponse> records=recordMapper.findPage(checkerId,q.getMatchId(),result,q.getStartTime(),q.getEndTime(),(long)(q.getPage()-1)*q.getSize(),q.getSize()).stream().map(this::response).toList();
        return new PageResponse<>(records,total,q.getPage(),q.getSize());
    }
    private CheckinResponse record(AuthenticatedUser user,MatchInfo match,ETicket ticket,String code,String result,String message,LocalDateTime now){
        CheckinRecord r=new CheckinRecord();r.setMatchId(match.getMatchId());r.setTicketId(ticket==null?null:ticket.getTicketId());r.setScannedTicketCode(code);r.setCheckerId(user.userId());r.setCheckResult(result);r.setCheckTime(now);r.setRemark(message);
        if(recordMapper.insert(r)!=1)throw new BusinessException(HttpStatus.CONFLICT,"failed to save check-in record");
        return new CheckinResponse(r.getCheckinId(),result,message,code,r.getTicketId(),match.getMatchId(),matchName(match),ticket==null?null:ticket.getRowNo(),ticket==null?null:ticket.getSeatNo(),now);
    }
    private String ticketResult(String status){return switch(status){case "UNUSED"->null;case "USED"->"TICKET_USED";case "REFUNDED"->"TICKET_REFUNDED";case "VOID"->"TICKET_VOID";default->"ORDER_INVALID";};}
    private String stateMessage(ETicket ticket){return switch(ticket.getTicketStatus()){case "USED"->"ticket was used at "+ticket.getUsedAt();case "REFUNDED"->"ticket has been refunded";case "VOID"->"ticket is void";default->"ticket state is invalid";};}
    private Long clubScope(AuthenticatedUser user){if("ADMIN".equals(user.roleCode()))return null;if(!"CHECKER".equals(user.roleCode()))throw new BusinessException(HttpStatus.FORBIDDEN,"checker or admin role is required");return requireClub(user);}
    private Long requireClub(AuthenticatedUser user){if(user.clubId()==null)throw new BusinessException(HttpStatus.FORBIDDEN,"checker account is not bound to a club");return user.clubId();}
    private void requireMatchScope(AuthenticatedUser user,MatchInfo match){Long club=clubScope(user);if(club!=null&&!club.equals(match.getHomeClubId()))throw new BusinessException(HttpStatus.FORBIDDEN,"checker cannot access another club's home match");}
    private CheckerMatchResponse matchResponse(MatchInfo m){return new CheckerMatchResponse(m.getMatchId(),m.getHomeClubId(),m.getHomeClubName(),m.getAwayClubName(),m.getStadiumName(),m.getMatchTime(),m.getMatchStatus(),OPEN_MATCH_STATUSES.contains(m.getMatchStatus()));}
    private CheckinRecordResponse response(CheckinRecord r){return new CheckinRecordResponse(r.getCheckinId(),r.getMatchId(),r.getHomeClubName()+" vs "+r.getAwayClubName(),r.getMatchTime(),r.getStadiumName(),r.getTicketId(),r.getScannedTicketCode(),r.getTicketStatus(),r.getCheckerId(),r.getCheckerUsername(),r.getCheckerName(),r.getCheckResult(),r.getRemark(),r.getCheckTime(),r.getZoneName(),r.getRowNo(),r.getSeatNo());}
    private String matchName(MatchInfo m){return m.getHomeClubName()+" vs "+m.getAwayClubName();}
    private String result(String value){if(value==null||value.isBlank())return null;String result=value.trim().toUpperCase(Locale.ROOT);if(!RESULTS.contains(result))throw new BusinessException("invalid checkResult");return result;}
    private String matchStatus(String value){if(value==null||value.isBlank())return null;String status=value.trim().toUpperCase(Locale.ROOT);if(!MATCH_STATUSES.contains(status))throw new BusinessException("invalid matchStatus");return status;}
    private void validateRange(CheckinQueryRequest q){if(q.getStartTime()!=null&&q.getEndTime()!=null&&q.getEndTime().isBefore(q.getStartTime()))throw new BusinessException("endTime must not be before startTime");}
}
