package com.example.leagueticket.service;

import java.time.LocalDateTime;

public record SystemTimeChangedEvent(LocalDateTime systemTime, String operationType) {}
