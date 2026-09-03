package com.example.leagueticket.service;

import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class TicketSalePolicy {
    private final SystemConfigMapper configMapper;
    private final SystemTimeService systemTimeService;

    public LocalDateTime calculateSaleStartTime(LocalDateTime matchTime) {
        if (matchTime == null) throw new BusinessException("matchTime is required");
        return matchTime.toLocalDate().minusDays(7).atTime(LocalTime.of(20, 0));
    }

    public void validateSaleWindow(LocalDateTime matchTime, LocalDateTime saleEndTime) {
        LocalDateTime saleStartTime = calculateSaleStartTime(matchTime);
        if (saleEndTime == null || !saleEndTime.isAfter(saleStartTime)) {
            throw new BusinessException("停售时间必须晚于自动开售时间");
        }
        int stopMinutes = saleStopMinutes();
        if (saleEndTime.isAfter(matchTime.minusMinutes(stopMinutes))) {
            throw new BusinessException("saleEndTime must not be later than " + stopMinutes + " minutes before match start");
        }
    }

    public void validateRescheduleWindow(LocalDateTime matchTime, LocalDateTime saleEndTime) {
        LocalDateTime saleStartTime = calculateSaleStartTime(matchTime);
        if (saleEndTime == null || !saleEndTime.isAfter(saleStartTime)) {
            throw new BusinessException(HttpStatus.CONFLICT, "比赛改期后自动开售时间不早于现有停售时间，请先调整票区停售时间");
        }
        int stopMinutes = saleStopMinutes();
        if (saleEndTime.isAfter(matchTime.minusMinutes(stopMinutes))) {
            throw new BusinessException(HttpStatus.CONFLICT, "比赛改期后现有停售时间晚于赛前停售边界，请先调整票区停售时间");
        }
    }

    public SaleEvaluation evaluateSaleAvailability(MatchInfo match, MatchTicketZone zone, long availableInventory) {
        return evaluateSaleAvailability(match, zone, systemTimeService.now(), availableInventory);
    }

    public SaleEvaluation evaluateSaleAvailability(MatchInfo match, MatchTicketZone zone,
                                                    LocalDateTime systemNow, long availableInventory) {
        if (!"PUBLISHED".equals(match.getMatchStatus())) return new SaleEvaluation(false, "MATCH_UNAVAILABLE");
        if ("DRAFT".equals(zone.getZoneStatus())) return new SaleEvaluation(false, "NOT_ENABLED");
        if ("PAUSED".equals(zone.getZoneStatus())) return new SaleEvaluation(false, "PAUSED");
        if ("CLOSED".equals(zone.getZoneStatus())) return new SaleEvaluation(false, "CLOSED");
        if (!"ON_SALE".equals(zone.getZoneStatus())) return new SaleEvaluation(false, "NOT_ENABLED");
        LocalDateTime autoSaleStart = calculateSaleStartTime(match.getMatchTime());
        if (systemNow.isBefore(autoSaleStart)) return new SaleEvaluation(false, "NOT_STARTED");
        if (zone.getSaleEndTime() == null || !systemNow.isBefore(zone.getSaleEndTime())) return new SaleEvaluation(false, "ENDED");
        if (!systemNow.isBefore(match.getMatchTime().minusMinutes(saleStopMinutes()))) return new SaleEvaluation(false, "ENDED");
        if (availableInventory <= 0) return new SaleEvaluation(false, "SOLD_OUT");
        return new SaleEvaluation(true, "AVAILABLE");
    }

    public void requireSaleAvailable(MatchInfo match, MatchTicketZone zone, long availableInventory) {
        SaleEvaluation evaluation = evaluateSaleAvailability(match, zone, availableInventory);
        if (!evaluation.available()) {
            throw new BusinessException(HttpStatus.CONFLICT, switch (evaluation.state()) {
                case "MATCH_UNAVAILABLE" -> "match is not available for ticket sales";
                case "NOT_ENABLED" -> "match ticket zone is not enabled for automatic sales";
                case "PAUSED" -> "match ticket zone sales are paused";
                case "CLOSED" -> "match ticket zone sales are closed";
                case "NOT_STARTED" -> "ticket sales have not started";
                case "ENDED" -> "ticket sales have ended";
                case "SOLD_OUT" -> "no AVAILABLE seats";
                default -> "ticket sales are not available";
            });
        }
    }

    public int saleStopMinutes() {
        String value = configMapper.findEnabledValue("SALE_STOP_BEFORE_MINUTES");
        try {
            return value == null ? 30 : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 30;
        }
    }

    public record SaleEvaluation(boolean available, String state) {}
}
