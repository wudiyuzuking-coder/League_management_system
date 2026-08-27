package com.example.leagueticket.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Result<T> {

    private final int code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(200, "success", null);
    }

    public static Result<Void> failure(int code, String message) {
        return new Result<>(code, message, null);
    }
}
