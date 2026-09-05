package com.example.leagueticket.exception;

import org.springframework.http.HttpStatus;

public class OrderExpiredException extends BusinessException {
    public OrderExpiredException() {
        super(HttpStatus.CONFLICT, "order has expired and was cancelled");
    }
}
