package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.MatchTicketZoneMapper;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.MatchResultReminderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.temporal.ChronoUnit;

@Service @Profile("dev") @RequiredArgsConstructor
public class MatchInfoServiceImpl implements MatchInfoService {
    private static final Set<String> STATUSES=Set.of("DRAFT","PUBLISHED","IN_PROGRESS","FINISHED","CANCELLED");
    private static final Map<String,Set<String>> NEXT=Map.of(
            "DRAFT",Set.of("PUBLISHED","CANCELLED"),
            "PUBLISHED",Set.of("IN_PROGRESS","CANCELLED"),
            "IN_PROGRESS",Set.of("FINISHED","CANCELLED"),
            "FINISHED",Set.of(),"CANCELLED",Set.of());
    private final MatchInfoMapper mapper;
    private final MatchTicketZoneMapper ticketZoneMapper;
    private final SeasonInfoService seasonService;
    private final RoundInfoService roundService;
    private final ClubInfoService clubService;
    private final StadiumInfoService stadiumService;
    private final SystemTimeService systemTimeService;
    private final TicketSalePolicy ticketSalePolicy;

    public PageResponse<MatchInfo> list(MatchQueryRequest query){validateQuery(query);long total=mapper.count(query);long offset=(long)(query.getPage()-1)*query.getSize();return new PageResponse<>(mapper.findPage(query,offset,query.getSize()),total,query.getPage(),query.getSize());}
    public PageResponse<MatchInfo> listPublic(MatchQueryRequest query){query.setPublicOnly(true);return list(query);}
    public MatchInfo getById(Long id){MatchInfo value=mapper.findById(id);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");return value;}
    public MatchInfo getPublicById(Long id){MatchInfo value=getById(id);if(!Set.of("PUBLISHED","IN_PROGRESS","FINISHED").contains(value.getMatchStatus()))throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");return value;}
    @Transactional public MatchInfo create(MatchRequest request){validateBusiness(request,null);SeasonInfo season=seasonService.getById(request.seasonId());MatchInfo match=copy(new MatchInfo(),request);match.setSaleStartTime(seasonSaleStart(season));match.setSaleEndTime(ticketSalePolicy.calculateSaleEndTime(request.matchTime()));ticketSalePolicy.validateSaleWindow(match);mapper.insert(match);return getById(match.getMatchId());}
    @Transactional public MatchInfo update(Long id,MatchRequest request){MatchInfo current=mapper.findByIdForUpdate(id);if(current==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match not found");if(Set.of("IN_PROGRESS","FINISHED","CANCELLED").contains(current.getMatchStatus()))throw new BusinessException("match basic information cannot be changed in current status");if("PUBLISHED".equals(current.getMatchStatus())&&!sameExceptTime(current,request))throw new BusinessException("published match only allows match time changes");validateBusiness(request,id);ticketZoneMapper.findByMatchForUpdate(id);MatchInfo changed=copy(current,request);changed.setSaleStartTime(current.getSaleStartTime()!=null?current.getSaleStartTime():seasonSaleStart(seasonService.getById(request.seasonId())));changed.setSaleEndTime(ticketSalePolicy.calculateSaleEndTime(request.matchTime()));ticketSalePolicy.validateSaleWindow(changed);if("PUBLISHED".equals(current.getMatchStatus()))mapper.updateTimeAndSale(changed);else mapper.updateBasic(changed);ticketZoneMapper.updateSaleWindowByMatch(id,changed.getSaleStartTime(),changed.getSaleEndTime());return getById(id);}
    @Transactional public MatchInfo updateStatus(Long id,String status){MatchInfo match=getById(id);if(!STATUSES.contains(status))throw new BusinessException("invalid match status");if("FINISHED".equals(status))throw new BusinessException(HttpStatus.CONFLICT,"比赛只能通过多人赛果确认流程结束");if(match.getMatchStatus().equals(status))return match;if(!NEXT.get(match.getMatchStatus()).contains(status))throw new BusinessException("invalid match status transition: "+match.getMatchStatus()+" -> "+status);if("PUBLISHED".equals(status))mapper.publish(id,systemTimeService.now());else mapper.updateStatus(id,status);return getById(id);}
    public PageResponse<MatchResultReminderResponse> resultReminders(MatchResultReminderQueryRequest query){String type=normalizeReminderType(query.getReminderType());var systemDate=systemTimeService.now().toLocalDate();long total=mapper.countResultReminders(systemDate,query.getSeasonId(),type);long offset=(long)(query.getPage()-1)*query.getSize();var records=mapper.findResultReminders(systemDate,query.getSeasonId(),type,offset,query.getSize()).stream().map(match->{var matchDate=match.getMatchTime().toLocalDate();String reminderType=matchDate.equals(systemDate)?"TODAY":"OVERDUE";long days=ChronoUnit.DAYS.between(matchDate,systemDate);return new MatchResultReminderResponse(match.getMatchId(),match.getSeasonId(),match.getSeasonName(),match.getRoundId(),match.getRoundNo(),match.getRoundName(),match.getHomeClubId(),match.getHomeClubName(),match.getAwayClubId(),match.getAwayClubName(),match.getMatchTime(),match.getMatchStatus(),match.getHomeScore(),match.getAwayScore(),reminderType,days);}).toList();return new PageResponse<>(records,total,query.getPage(),query.getSize());}
    private void validateBusiness(MatchRequest r,Long excludeId){SeasonInfo season=seasonService.getById(r.seasonId());RoundInfo round=roundService.getById(r.roundId());if(!round.getSeasonId().equals(r.seasonId()))throw new BusinessException("round does not belong to season");ClubInfo home=clubService.getById(r.homeClubId());clubService.getById(r.awayClubId());if(r.homeClubId().equals(r.awayClubId()))throw new BusinessException("home club and away club must be different");StadiumInfo stadium=stadiumService.getById(r.stadiumId());if(!"ACTIVE".equals(stadium.getStadiumStatus()))throw new BusinessException("disabled stadium cannot host a new or edited match");if(home.getHomeStadiumId()==null)throw new BusinessException("home club has no home stadium configured");if(!home.getHomeStadiumId().equals(r.stadiumId()))throw new BusinessException("stadium must be the home club's home stadium");if(round.getStartDate()==null||round.getEndDate()==null)throw new BusinessException("round dates must be configured before creating matches");var date=r.matchTime().toLocalDate();if(date.isBefore(round.getStartDate())||date.isAfter(round.getEndDate()))throw new BusinessException("match time must be within round date range");if(date.isBefore(season.getStartDate())||date.isAfter(season.getEndDate()))throw new BusinessException("match time must be within season date range");if(mapper.countDuplicate(r.seasonId(),r.roundId(),r.homeClubId(),r.awayClubId(),excludeId)>0)throw new BusinessException(HttpStatus.CONFLICT,"same fixture already exists in this round");}
    private boolean sameExceptTime(MatchInfo m,MatchRequest r){return Objects.equals(m.getSeasonId(),r.seasonId())&&Objects.equals(m.getRoundId(),r.roundId())&&Objects.equals(m.getHomeClubId(),r.homeClubId())&&Objects.equals(m.getAwayClubId(),r.awayClubId())&&Objects.equals(m.getStadiumId(),r.stadiumId());}
    private MatchInfo copy(MatchInfo m,MatchRequest r){m.setSeasonId(r.seasonId());m.setRoundId(r.roundId());m.setHomeClubId(r.homeClubId());m.setAwayClubId(r.awayClubId());m.setStadiumId(r.stadiumId());m.setMatchTime(r.matchTime());return m;}
    private java.time.LocalDateTime seasonSaleStart(SeasonInfo season){if(season.getTicketSaleStartTime()!=null)return season.getTicketSaleStartTime();if(season.getRegistrationDeadline()!=null)return season.getRegistrationDeadline().toLocalDate().plusDays(1).atTime(20,0);throw new BusinessException("赛季自动售票时间尚未生成");}
    private void validateQuery(MatchQueryRequest q){if(q.getMatchStatus()!=null&&!q.getMatchStatus().isBlank()&&!STATUSES.contains(q.getMatchStatus()))throw new BusinessException("invalid match status");if(q.getStartTime()!=null&&q.getEndTime()!=null&&q.getEndTime().isBefore(q.getStartTime()))throw new BusinessException("endTime must not be before startTime");}
    private String normalizeReminderType(String value){if(value==null||value.isBlank())return null;String type=value.trim().toUpperCase();if(!Set.of("TODAY","OVERDUE").contains(type))throw new BusinessException("invalid reminderType");return type;}
}
