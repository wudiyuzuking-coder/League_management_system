package com.example.leagueticket.vo;

public record PaymentResponse(PaymentSummaryResponse payment,OrderDetailResponse orderDetail,boolean idempotent) {}
