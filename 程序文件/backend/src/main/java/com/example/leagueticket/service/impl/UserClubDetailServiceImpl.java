package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.SeasonInfo;
import com.example.leagueticket.entity.StadiumInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubInfoMapper;
import com.example.leagueticket.mapper.CoachInfoMapper;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.PlayerInfoMapper;
import com.example.leagueticket.mapper.SeasonInfoMapper;
import com.example.leagueticket.service.ClubSeasonRecordService;
import com.example.leagueticket.service.StadiumInfoService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.service.UserClubDetailService;
import com.example.leagueticket.vo.StandingResponse;
import com.example.leagueticket.vo.UserClubDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class UserClubDetailServiceImpl implements UserClubDetailService {
    private final ClubInfoMapper clubMapper;
    private final PlayerInfoMapper playerMapper;
    private final CoachInfoMapper coachMapper;
    private final MatchInfoMapper matchMapper;
    private final SeasonInfoMapper seasonMapper;
    private final ClubSeasonRecordService recordService;
    private final StadiumInfoService stadiumService;
    private final SystemTimeService timeService;

    @Override
    public UserClubDetailResponse detail(Long clubId) {
        ClubInfo club = clubMapper.findActiveById(clubId);
        if (club == null) throw new BusinessException(HttpStatus.NOT_FOUND, "club not found");

        var now = timeService.now();
        LocalDate systemDate = now.toLocalDate();
        var players = playerMapper.findActiveByClubId(clubId).stream()
                .map(player -> new UserClubDetailResponse.Player(
                        player.getPlayerId(), player.getPlayerName(), player.getShirtNo(),
                        player.getPosition(), age(player.getBirthDate(), systemDate), player.getNationality()))
                .toList();
        var coaches = coachMapper.findActiveByClubId(clubId).stream()
                .map(coach -> new UserClubDetailResponse.Coach(
                        coach.getCoachId(), coach.getCoachName(), coach.getTitle(), coach.getNationality()))
                .toList();

        SeasonInfo season = seasonMapper.findCurrentPublicForClub(clubId, systemDate);
        if (season == null) season = seasonMapper.findLatestPublicForClub(clubId);
        UserClubDetailResponse.Standing standing = standing(clubId, season);

        List<UserClubDetailResponse.Match> recent = matchMapper.findRecentPublicByClub(clubId, 5)
                .stream().map(this::match).toList();
        List<UserClubDetailResponse.Match> upcoming = matchMapper.findUpcomingPublicByClub(clubId, now, 5)
                .stream().map(this::match).toList();
        UserClubDetailResponse.Match next = upcoming.isEmpty() ? null : upcoming.get(0);
        Long days = next == null ? null : Math.max(0L,
                ChronoUnit.DAYS.between(systemDate, next.matchTime().toLocalDate()));

        return new UserClubDetailResponse(club(club), players, coaches, standing,
                recent, upcoming, next, days);
    }

    private UserClubDetailResponse.Club club(ClubInfo club) {
        UserClubDetailResponse.Stadium stadium = null;
        if (club.getHomeStadiumId() != null) {
            StadiumInfo value = stadiumService.getById(club.getHomeStadiumId());
            stadium = new UserClubDetailResponse.Stadium(value.getStadiumId(), value.getStadiumName(),
                    value.getCity(), value.getAddress(), value.getCapacity(), value.getLayoutDesc());
        }
        return new UserClubDetailResponse.Club(club.getClubId(), club.getClubName(), club.getShortName(),
                club.getLogoUrl(), club.getHomeCity(), club.getDescription(), stadium);
    }

    private UserClubDetailResponse.Standing standing(Long clubId, SeasonInfo season) {
        if (season == null) return null;
        StandingResponse row = recordService.standings(season.getSeasonId()).stream()
                .filter(value -> clubId.equals(value.clubId())).findFirst().orElse(null);
        if (row == null) return null;
        return new UserClubDetailResponse.Standing(season.getSeasonId(), season.getSeasonName(),
                row.matchesPlayed(), row.wins(), row.draws(), row.losses(), row.goalsFor(),
                row.goalsAgainst(), row.goalDifference(), row.points(), row.rank());
    }

    private UserClubDetailResponse.Match match(MatchInfo value) {
        boolean finished = "FINISHED".equals(value.getMatchStatus());
        return new UserClubDetailResponse.Match(value.getMatchId(), value.getSeasonId(), value.getSeasonName(),
                value.getHomeClubId(), value.getHomeClubName(), value.getAwayClubId(), value.getAwayClubName(),
                value.getMatchTime(), value.getStadiumId(), value.getStadiumName(),
                finished ? value.getHomeScore() : null, finished ? value.getAwayScore() : null,
                value.getMatchStatus());
    }

    private Integer age(LocalDate birthDate, LocalDate systemDate) {
        if (birthDate == null) return null;
        return Math.max(0, Period.between(birthDate, systemDate).getYears());
    }
}
