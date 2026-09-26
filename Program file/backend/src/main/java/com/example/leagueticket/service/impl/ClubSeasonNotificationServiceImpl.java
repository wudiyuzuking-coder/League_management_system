package com.example.leagueticket.service.impl;

import com.example.leagueticket.mapper.ClubSeasonNotificationMapper;
import com.example.leagueticket.service.ClubSeasonNotificationService;
import com.example.leagueticket.vo.SeasonCancellationNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class ClubSeasonNotificationServiceImpl implements ClubSeasonNotificationService {
    public static final String CANCELLATION_MESSAGE = "因参赛俱乐部不足 2 支，该赛季已取消";

    private final ClubSeasonNotificationMapper mapper;

    @Override
    @Transactional
    public int createCancellationNotifications(Long seasonId, LocalDateTime occurredAt) {
        return mapper.insertCancellationNotifications(seasonId, CANCELLATION_MESSAGE, occurredAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonCancellationNotificationResponse> listForClub(Long clubId) {
        return mapper.findByClub(clubId);
    }
}
