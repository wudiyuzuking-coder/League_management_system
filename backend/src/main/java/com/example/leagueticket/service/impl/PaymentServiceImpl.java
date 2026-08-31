package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PaymentRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.*;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final MatchSeatInventoryMapper inventoryMapper;
    private final PaymentRecordMapper paymentMapper;
    private final ETicketMapper ticketMapper;
    private final OrderService orderService;
    private final SystemTimeService systemTimeService;

    @Override
    @Transactional(noRollbackFor=OrderExpiredException.class)
    public PaymentResponse pay(Long userId,Long orderId,PaymentRequest request){
        String method=request.payMethod().trim().toUpperCase(Locale.ROOT);
        String result=request.simulateResult().trim().toUpperCase(Locale.ROOT);
        if(!"SIMULATED".equals(method))throw new BusinessException("only SIMULATED payment is supported");
        if(!Set.of("SUCCESS","FAILED").contains(result))throw new BusinessException("simulateResult must be SUCCESS or FAILED");

        TicketOrder order=orderMapper.findByIdForUpdate(orderId);
        if(order==null)throw new BusinessException(HttpStatus.NOT_FOUND,"order not found");
        if(!order.getUserId().equals(userId))throw new BusinessException(HttpStatus.FORBIDDEN,"cannot pay another user's order");

        if("PAID".equals(order.getOrderStatus())){
            PaymentRecord success=paymentMapper.findSuccessByOrder(orderId);
            if(success==null)throw new BusinessException(HttpStatus.CONFLICT,"paid order has no successful payment record");
            return new PaymentResponse(payment(success),orderService.detailOwned(userId,orderId),true);
        }
        if(!"PENDING_PAYMENT".equals(order.getOrderStatus()))throw new BusinessException(HttpStatus.CONFLICT,"order status cannot be paid");
        LocalDateTime now=systemTimeService.now();
        if(!order.getExpireTime().isAfter(now)){
            orderService.closeExpiredOrder(orderId);
            throw new OrderExpiredException();
        }
        int expected=order.getTicketCount();
        if(itemMapper.countByOrder(orderId)!=expected||itemMapper.countLockedByOrder(orderId)!=expected||
                inventoryMapper.countPayableByOrder(orderId)!=expected||paymentMapper.findSuccessByOrder(orderId)!=null)
            throw new BusinessException(HttpStatus.CONFLICT,"order payment data is inconsistent");

        PaymentRecord payment=new PaymentRecord();
        payment.setPaymentNo(code("PAY",32,now));payment.setOrderId(orderId);payment.setPayAmount(order.getTotalAmount());payment.setPayMethod(method);payment.setCreatedAt(now);
        if(paymentMapper.insert(payment)!=1)throw new BusinessException(HttpStatus.CONFLICT,"failed to create payment record");
        if("FAILED".equals(result)){
            if(paymentMapper.finish(payment.getPaymentId(),"FAILED",null,null)!=1)throw new BusinessException(HttpStatus.CONFLICT,"failed to record simulated payment result");
            payment=paymentMapper.findLatestByOrder(orderId);
            return new PaymentResponse(payment(payment),orderService.detailOwned(userId,orderId),false);
        }

        String tradeNo=code("SIM",64,now);
        if(paymentMapper.finish(payment.getPaymentId(),"SUCCESS",tradeNo,now)!=1||orderMapper.markPaid(orderId,now)!=1||
                itemMapper.markPaid(orderId)!=expected||inventoryMapper.markSoldByOrder(orderId)!=expected)
            throw new BusinessException(HttpStatus.CONFLICT,"payment state update was incomplete and has been rolled back");
        for(OrderItem item:itemMapper.findByOrder(orderId)){
            if(!"PAID".equals(item.getItemStatus()))throw new BusinessException(HttpStatus.CONFLICT,"paid item state is inconsistent");
            ETicket ticket=new ETicket();ticket.setTicketCode(code("ET",64,now));ticket.setOrderId(orderId);ticket.setItemId(item.getItemId());ticket.setIssuedAt(now);
            if(ticketMapper.insert(ticket)!=1)throw new BusinessException(HttpStatus.CONFLICT,"electronic ticket generation failed and payment was rolled back");
        }
        if(ticketMapper.findByOrder(orderId).size()!=expected)throw new BusinessException(HttpStatus.CONFLICT,"electronic ticket generation was incomplete and payment was rolled back");
        PaymentRecord success=paymentMapper.findSuccessByOrder(orderId);
        return new PaymentResponse(payment(success),orderService.detailOwned(userId,orderId),false);
    }
    private static PaymentSummaryResponse payment(PaymentRecord p){return new PaymentSummaryResponse(p.getPaymentId(),p.getPaymentNo(),p.getOrderId(),p.getPayAmount(),p.getPayMethod(),p.getPayStatus(),p.getThirdPartyTradeNo(),p.getPayTime(),p.getCreatedAt());}
    private static String code(String prefix,int max,LocalDateTime now){String value=prefix+now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);return value.substring(0,Math.min(max,value.length()));}
}
