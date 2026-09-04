package com.example.leagueticket.service;

import com.example.leagueticket.dto.PaymentRequest;
import com.example.leagueticket.vo.PaymentResponse;

public interface PaymentService {
    PaymentResponse pay(Long userId,Long orderId,PaymentRequest request);
}
