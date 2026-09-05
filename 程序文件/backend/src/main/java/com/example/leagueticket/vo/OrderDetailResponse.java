package com.example.leagueticket.vo;

import java.util.List;

public record OrderDetailResponse(OrderSummaryResponse order,List<OrderItemResponse> items,
        PaymentSummaryResponse payment,List<ETicketResponse> tickets) {}
