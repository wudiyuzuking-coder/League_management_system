package com.example.leagueticket.vo;

import java.math.BigDecimal;

public record OrderItemResponse(Long itemId,Long inventoryId,String rowLabel,String seatLabel,
                                BigDecimal unitPrice,String itemStatus) {}
