package com.example.leagueticket.service;

import java.time.LocalDate;

public interface PersonnelAgeValidationService {
    int validatePlayerAge(LocalDate birthDate, Integer birthYear);
    int validateCoachAge(Integer birthYear);
}
