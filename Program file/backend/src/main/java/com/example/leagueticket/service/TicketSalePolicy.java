package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service @Profile("dev") @RequiredArgsConstructor
public class TicketSalePolicy {
    private final SystemTimeService systemTimeService;
    public LocalDateTime calculateSaleStartTime(MatchInfo match){if(match==null)return calculateSaleStartTime((LocalDateTime)null);return calculateSaleStartTime(match.getMatchTime());}
    public LocalDateTime calculateSaleStartTime(LocalDateTime matchTime){if(matchTime==null)throw new BusinessException("matchTime is required");return matchTime.toLocalDate().minusDays(14).atTime(LocalTime.of(20,0));}
    public LocalDateTime calculateSaleEndTime(LocalDateTime matchTime){if(matchTime==null)throw new BusinessException("matchTime is required");return matchTime.minusHours(1);}
    public void applySaleWindow(MatchInfo match){match.setSaleStartTime(calculateSaleStartTime(match));match.setSaleEndTime(calculateSaleEndTime(match.getMatchTime()));}
    public void validateSaleWindow(MatchInfo match){LocalDateTime expectedStart=calculateSaleStartTime(match),expectedEnd=calculateSaleEndTime(match.getMatchTime());if(!expectedStart.equals(match.getSaleStartTime()))throw new BusinessException("开售时间必须为比赛日期前14天20:00");if(!expectedEnd.equals(match.getSaleEndTime()))throw new BusinessException("停售时间必须为比赛开始前1小时");if(!expectedEnd.isAfter(expectedStart))throw new BusinessException("自动停售时间必须晚于自动开售时间");}
    public SaleEvaluation evaluateSaleAvailability(MatchInfo match,MatchTicketZone zone,long availableInventory){return evaluateSaleAvailability(match,zone,systemTimeService.now(),availableInventory);}
    public SaleEvaluation evaluateSaleAvailability(MatchInfo match,MatchTicketZone zone,LocalDateTime systemNow,long availableInventory){if(!"PUBLISHED".equals(match.getMatchStatus()))return new SaleEvaluation(false,"MATCH_UNAVAILABLE");if("DRAFT".equals(zone.getZoneStatus()))return new SaleEvaluation(false,"NOT_ENABLED");if("PAUSED".equals(zone.getZoneStatus()))return new SaleEvaluation(false,"PAUSED");if("CLOSED".equals(zone.getZoneStatus()))return new SaleEvaluation(false,"CLOSED");if(!"ON_SALE".equals(zone.getZoneStatus()))return new SaleEvaluation(false,"NOT_ENABLED");return evaluateWindow(match,systemNow,availableInventory,true);}
    public SaleEvaluation evaluateMatchAvailability(MatchInfo match,LocalDateTime systemNow,long availableInventory,boolean hasOnSaleZone){return evaluateWindow(match,systemNow,availableInventory,hasOnSaleZone);}
    private SaleEvaluation evaluateWindow(MatchInfo match,LocalDateTime systemNow,long availableInventory,boolean enabled){if(!"PUBLISHED".equals(match.getMatchStatus()))return new SaleEvaluation(false,"MATCH_UNAVAILABLE");LocalDateTime start=calculateSaleStartTime(match),end=calculateSaleEndTime(match.getMatchTime());if(systemNow.isBefore(start))return new SaleEvaluation(false,"NOT_STARTED");if(!systemNow.isBefore(end))return new SaleEvaluation(false,"ENDED");if(!enabled)return new SaleEvaluation(false,"NOT_ENABLED");if(availableInventory<=0)return new SaleEvaluation(false,"SOLD_OUT");return new SaleEvaluation(true,"AVAILABLE");}
    public void requireSaleAvailable(MatchInfo match,MatchTicketZone zone,long availableInventory){SaleEvaluation evaluation=evaluateSaleAvailability(match,zone,availableInventory);if(!evaluation.available())throw new BusinessException(HttpStatus.CONFLICT,switch(evaluation.state()){case "MATCH_UNAVAILABLE"->"match is not available for ticket sales";case "NOT_ENABLED"->"match ticket zone is not enabled for automatic sales";case "PAUSED"->"match ticket zone sales are paused";case "CLOSED"->"match ticket zone sales are closed";case "NOT_STARTED"->"ticket sales have not started";case "ENDED"->"ticket sales have ended";case "SOLD_OUT"->"no AVAILABLE seats";default->"ticket sales are not available";});}
    public int saleStopMinutes(){return 60;}
    public record SaleEvaluation(boolean available,String state){}
}
