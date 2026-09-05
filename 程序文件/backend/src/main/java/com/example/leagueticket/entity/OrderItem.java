package com.example.leagueticket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItem {
    private Long itemId;
    private Long orderId;
    private Long inventoryId;
    private BigDecimal ticketPrice;
    private String zoneNameSnapshot;
    private String rowNoSnapshot;
    private String seatNoSnapshot;
    private String itemStatus;
    private LocalDateTime createdAt;
}
