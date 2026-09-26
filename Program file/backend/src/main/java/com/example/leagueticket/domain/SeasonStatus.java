package com.example.leagueticket.domain;

import com.example.leagueticket.exception.BusinessException;

import java.util.Locale;

public enum SeasonStatus {
    DRAFT,
    REGISTRATION,
    PREPARING,
    IN_PROGRESS,
    FINISHED,
    CANCELLED;

    public static SeasonStatus fromStored(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("season status is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("REGISTERING".equals(normalized)) normalized = REGISTRATION.name();
        if ("ACTIVE".equals(normalized)) normalized = IN_PROGRESS.name();
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("invalid season status: " + value);
        }
    }

    public boolean matches(String value) {
        return this == fromStored(value);
    }
}
