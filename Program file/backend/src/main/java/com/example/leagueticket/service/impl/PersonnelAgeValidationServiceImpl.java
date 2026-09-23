package com.example.leagueticket.service.impl;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.PersonnelAgeValidationService;
import com.example.leagueticket.service.SystemTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class PersonnelAgeValidationServiceImpl implements PersonnelAgeValidationService {
    private final SystemTimeService timeService;

    @Override
    public int validatePlayerAge(LocalDate birthDate, Integer birthYear) {
        int year = resolveBirthYear(birthDate, birthYear, "球员出生年份不能为空");
        int age = timeService.now().getYear() - year;
        if (age < 18 || age > 50) {
            throw new BusinessException("球员年龄必须在18至50岁之间");
        }
        return age;
    }

    @Override
    public int validateCoachAge(Integer birthYear) {
        if (birthYear == null) {
            throw new BusinessException("教练出生年份不能为空");
        }
        int age = timeService.now().getYear() - birthYear;
        if (age < 18 || age > 100) {
            throw new BusinessException("教练年龄必须在18至100岁之间");
        }
        return age;
    }

    private int resolveBirthYear(LocalDate birthDate, Integer birthYear, String missingMessage) {
        if (birthYear != null) return birthYear;
        if (birthDate != null) return birthDate.getYear();
        throw new BusinessException(missingMessage);
    }
}
