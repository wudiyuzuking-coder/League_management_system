package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRecord {
    private Long paymentId;
    private String paymentNo;
    private Long orderId;
    private BigDecimal payAmount;
    private String payMethod;
    private String payStatus;
    private String thirdPartyTradeNo;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
