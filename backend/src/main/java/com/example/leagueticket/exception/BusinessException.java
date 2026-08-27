package com.example.leagueticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, 400, message);
    }

    public BusinessException(int code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    public BusinessException(HttpStatus httpStatus, String message) {
        this(httpStatus, httpStatus.value(), message);
    }

    public BusinessException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
