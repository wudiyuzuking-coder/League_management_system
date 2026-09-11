package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private static final Set<String> STATUSES=Set.of("APPROVED");
    private final RefundApplyMapper refundMapper;private final TicketOrderMapper orderMapper;private final OrderItemMapper itemMapper;private final ETicketMapper ticketMapper;private final MatchSeatInventoryMapper inventoryMapper;private final PaymentRecordMapper paymentMapper;private final MatchInfoMapper matchMapper;private final SystemConfigMapper configMapper;private final SystemTimeService systemTimeService;
    @Override @Transactional public RefundResponse apply(Long userId,Long orderId,RefundApplyRequest request){TicketOrder order=requireOrderForUpdate(orderId);if(!order.getUserId().equals(userId))throw new BusinessException(HttpStatus.FORBIDDEN,"cannot refund another user's order");if(!"PAID".equals(order.getOrderStatus()))throw new BusinessException(HttpStatus.CONFLICT,"only a PAID order can be refunded");MatchInfo match=matchMapper.findById(order.getMatchId());if(match==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");LocalDateTime now=systemTimeService.now();LocalDateTime deadline=match.getMatchTime().minusHours(configInt("REFUND_STOP_BEFORE_HOURS",24));if(!now.isBefore(deadline))throw new BusinessException(HttpStatus.CONFLICT,"refund deadline has passed");int expected=order.getTicketCount();if(itemMapper.countByOrder(orderId)!=expected||itemMapper.countStatusByOrder(orderId,"PAID")!=expected)throw new BusinessException(HttpStatus.CONFLICT,"order item states do not allow a refund");List<ETicket> tickets=ticketMapper.findByOrderForUpdate(orderId);if(tickets.size()!=expected||tickets.stream().anyMatch(t->!"UNUSED".equals(t.getTicketStatus())))throw new BusinessException(HttpStatus.CONFLICT,"all electronic tickets must be UNUSED");if(paymentMapper.findSuccessByOrder(orderId)==null)throw new BusinessException(HttpStatus.CONFLICT,"successful payment record not found");if(refundMapper.countByOrder(orderId)>0)throw new BusinessException(HttpStatus.CONFLICT,"a refund already exists for this order");BigDecimal rate=RefundPolicy.rate(now,match.getMatchTime());BigDecimal amount=order.getTotalAmount().multiply(rate).setScale(2,RoundingMode.HALF_UP);RefundApply refund=new RefundApply();refund.setRefundNo(number(now));refund.setOrderId(orderId);refund.setApplicantId(userId);refund.setCreatedAt(now);refund.setAuditTime(now);refund.setReason(request.reason().trim());refund.setRefundRate(rate);refund.setRefundAmount(amount);refund.setFeeAmount(order.getTotalAmount().subtract(amount));if(refundMapper.insert(refund)!=1||orderMapper.markRefunded(orderId)!=1||itemMapper.markRefunded(orderId)!=expected||ticketMapper.markRefunded(orderId)!=expected||inventoryMapper.releaseSoldByOrder(orderId)!=expected)throw new BusinessException(HttpStatus.CONFLICT,"automatic refund was incomplete and has been rolled back");return detail(refund.getRefundId());}
    public PageResponse<RefundResponse> listOwned(Long userId,RefundQueryRequest query){String status=status(query.getRefundStatus());long total=refundMapper.countOwned(userId,status);List<RefundResponse> records=refundMapper.findOwnedPage(userId,status,(long)(query.getPage()-1)*query.getSize(),query.getSize()).stream().map(this::response).toList();return new PageResponse<>(records,total,query.getPage(),query.getSize());}
    public RefundResponse detailOwned(Long userId,Long refundId){RefundApply r=refundMapper.findOwnedDetail(refundId,userId);if(r==null)throw new BusinessException(HttpStatus.NOT_FOUND,"refund not found");return response(r);}
    private TicketOrder requireOrderForUpdate(Long id){TicketOrder o=orderMapper.findByIdForUpdate(id);if(o==null)throw new BusinessException(HttpStatus.NOT_FOUND,"order not found");return o;}
    private RefundResponse detail(Long id){RefundApply r=refundMapper.findDetail(id);if(r==null)throw new BusinessException(HttpStatus.NOT_FOUND,"refund not found");return response(r);}
    private RefundResponse response(RefundApply r){List<ETicketResponse> tickets=ticketMapper.findByOrder(r.getOrderId()).stream().map(ETicketServiceImpl::response).toList();return new RefundResponse(r.getRefundId(),r.getRefundNo(),r.getOrderId(),r.getOrderNo(),r.getApplicantId(),r.getUsername(),r.getRefundAmount(),r.getRefundRate(),r.getFeeAmount(),r.getProcessingMode(),r.getReason(),r.getRefundStatus(),r.getCreatedAt(),r.getAuditorId(),r.getAuditTime(),r.getAuditRemark(),r.getMatchId(),r.getHomeClubName(),r.getAwayClubName(),r.getMatchTime(),r.getStadiumName(),r.getZoneName(),tickets);}
    private String status(String value){if(value==null||value.isBlank())return null;String s=value.trim().toUpperCase(Locale.ROOT);if(!STATUSES.contains(s))throw new BusinessException("invalid refundStatus");return s;}
    private int configInt(String key,int fallback){String v=configMapper.findEnabledValue(key);try{return v==null?fallback:Integer.parseInt(v);}catch(NumberFormatException ignored){return fallback;}}
    private String number(LocalDateTime now){String v="RF"+now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);return v.substring(0,32);}
}
