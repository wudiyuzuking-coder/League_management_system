package com.example.leagueticket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSummaryResponse(Long paymentId,String paymentNo,Long orderId,BigDecimal payAmount,
        String payMethod,String payStatus,String thirdPartyTradeNo,LocalDateTime payTime,LocalDateTime createdAt) {}
