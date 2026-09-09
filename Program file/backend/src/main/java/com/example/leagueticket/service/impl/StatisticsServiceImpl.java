package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.StatisticsMapper;
import com.example.leagueticket.service.StatisticsService;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
@Transactional(readOnly=true)
public class StatisticsServiceImpl implements StatisticsService {
    private static final Set<String> MATCH_STATUSES=Set.of("DRAFT","PUBLISHED","IN_PROGRESS","FINISHED","CANCELLED");
    private final StatisticsMapper mapper;

    public OverviewStatisticsResponse overview(StatisticsQueryRequest q){prepare(q);return mapper.overview(q,null);}
    public PageResponse<MatchStatisticsResponse> matches(StatisticsQueryRequest q){prepare(q);return page(q,null);}
    public MatchStatisticsResponse matchDetail(Long id){StatisticsQueryRequest q=new StatisticsQueryRequest();MatchStatisticsResponse r=mapper.matchDetail(id,q,null);if(r==null)throw new BusinessException(HttpStatus.NOT_FOUND,"match statistics not found");r.setZones(mapper.matchZoneStatistics(id));return r;}
    public List<ClubStatisticsResponse> clubs(StatisticsQueryRequest q){prepare(q);return mapper.clubStatistics(q,null);}
    public List<MatchStatisticsResponse> popular(PopularMatchesQueryRequest q){StatisticsQueryRequest filter=new StatisticsQueryRequest();filter.setSeasonId(q.getSeasonId());return mapper.popularMatches(filter,null,q.getLimit());}
    public List<SalesTrendResponse> salesTrend(StatisticsQueryRequest q){prepareRange(q);return mapper.salesTrend(q);}
    public RefundStatisticsResponse refunds(StatisticsQueryRequest q){prepareRange(q);return mapper.refundStatistics(q);}
    public CheckinStatisticsResponse checkins(StatisticsQueryRequest q){prepareRange(q);return mapper.checkinStatistics(q);}
    public ClubStatisticsResponse clubOverview(Long clubId,StatisticsQueryRequest q){requireClub(clubId);prepare(q);q.setClubId(null);List<ClubStatisticsResponse> rows=mapper.clubStatistics(q,clubId);if(rows.isEmpty())throw new BusinessException(HttpStatus.NOT_FOUND,"club statistics not found");return rows.get(0);}
    public PageResponse<MatchStatisticsResponse> clubMatches(Long clubId,StatisticsQueryRequest q){requireClub(clubId);prepare(q);q.setClubId(null);return page(q,clubId);}

    private PageResponse<MatchStatisticsResponse> page(StatisticsQueryRequest q,Long clubScope){long total=mapper.countMatches(q,clubScope);List<MatchStatisticsResponse> records=mapper.matchPage(q,clubScope,(long)(q.getPage()-1)*q.getSize(),q.getSize());return new PageResponse<>(records,total,q.getPage(),q.getSize());}
    private void prepare(StatisticsQueryRequest q){prepareRange(q);if(q.getMatchStatus()!=null&&!q.getMatchStatus().isBlank()){String s=q.getMatchStatus().trim().toUpperCase(Locale.ROOT);if(!MATCH_STATUSES.contains(s))throw new BusinessException("invalid matchStatus");q.setMatchStatus(s);}else q.setMatchStatus(null);}
    private void prepareRange(StatisticsQueryRequest q){if(q.getStartTime()!=null&&q.getEndTime()!=null&&q.getEndTime().isBefore(q.getStartTime()))throw new BusinessException("endTime must not be before startTime");}
    private void requireClub(Long clubId){if(clubId==null)throw new BusinessException(HttpStatus.FORBIDDEN,"club account is not bound to a club");}
}
