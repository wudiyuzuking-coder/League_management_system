package com.example.leagueticket.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.example.leagueticket.dto.OrderCreateRequest;
import com.example.leagueticket.dto.OrderQueryRequest;
import com.example.leagueticket.entity.MatchInfo;
import com.example.leagueticket.entity.MatchTicketZone;
import com.example.leagueticket.entity.OrderItem;
import com.example.leagueticket.entity.PaymentRecord;
import com.example.leagueticket.entity.RefundApply;
import com.example.leagueticket.entity.TicketOrder;
import com.example.leagueticket.entity.UserPrefilledPassenger;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ETicketMapper;
import com.example.leagueticket.mapper.MatchInfoMapper;
import com.example.leagueticket.mapper.MatchSeatInventoryMapper;
import com.example.leagueticket.mapper.MatchTicketZoneMapper;
import com.example.leagueticket.mapper.OrderItemMapper;
import com.example.leagueticket.mapper.PaymentRecordMapper;
import com.example.leagueticket.mapper.RefundApplyMapper;
import com.example.leagueticket.mapper.SystemConfigMapper;
import com.example.leagueticket.mapper.TicketOrderMapper;
import com.example.leagueticket.service.OrderService;
import com.example.leagueticket.service.StandardTicketZoneSelector;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.service.TicketPassengerService;
import com.example.leagueticket.service.TicketSalePolicy;
import com.example.leagueticket.vo.ETicketResponse;
import com.example.leagueticket.vo.OrderDetailResponse;
import com.example.leagueticket.vo.OrderItemResponse;
import com.example.leagueticket.vo.OrderSummaryResponse;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.PaymentSummaryResponse;
import com.example.leagueticket.vo.RefundResponse;
import com.example.leagueticket.vo.SeatAllocationResponse;

import lombok.RequiredArgsConstructor;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Set<String> QUERY_STATUSES = Set.of("PENDING_PAYMENT", "PAID", "CANCELLED", "REFUND_PENDING",
            "REFUNDED");
    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final MatchTicketZoneMapper zoneMapper;
    private final MatchInfoMapper matchMapper;
    private final MatchSeatInventoryMapper inventoryMapper;
    private final PaymentRecordMapper paymentMapper;
    private final ETicketMapper ticketMapper;
    private final RefundApplyMapper refundMapper;
    private final SystemConfigMapper configMapper;
    private final SystemTimeService systemTimeService;
    private final TicketSalePolicy ticketSalePolicy;
    private final SeatAllocateService seatAllocateService;
    private final StandardTicketZoneSelector standardTicketZoneSelector;
    private final TicketPassengerService ticketPassengerService;
    private final ObjectProvider<OrderService> orderServiceProvider;

    @Override
    @Transactional
    public OrderDetailResponse create(Long userId, OrderCreateRequest request) {
        int max = configInt("MAX_TICKETS_PER_ORDER", 4);
        if (request.ticketCount() == null)
            throw new BusinessException("ticketCount is required");
        if (request.ticketCount() < 1 || request.ticketCount() > max)
            throw new BusinessException("ticketCount must be between 1 and " + max);
        List<UserPrefilledPassenger> passengers = ticketPassengerService.requireForOrder(userId, request.passengerIds(),
                request.ticketCount());
        boolean legacy = request.matchZoneId() != null;
        boolean standard = request.matchId() != null && request.ticketType() != null && !request.ticketType().isBlank();
        if (legacy == standard)
            throw new BusinessException("provide either matchZoneId or matchId with ticketType");
        MatchTicketZone zone = legacy ? zoneMapper.findByIdForUpdate(request.matchZoneId())
                : standardTicketZoneSelector.select(request.matchId(), request.ticketType(), request.ticketCount(),
                        true);
        if (zone == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "match ticket zone not found");
        MatchInfo match = matchMapper.findById(zone.getMatchId());
        LocalDateTime now = systemTimeService.now();
        long availableInventory = inventoryMapper.countStatus(zone.getMatchZoneId(), "AVAILABLE");
        ticketSalePolicy.requireSaleAvailable(match, zone, availableInventory);
        if (availableInventory < request.ticketCount())
            throw new BusinessException(HttpStatus.CONFLICT, "not enough AVAILABLE seats");
        LocalDateTime expire = now.plusMinutes(configInt("ORDER_PAYMENT_TIMEOUT_MINUTES", 15));
        TicketOrder order = new TicketOrder();
        order.setOrderNo(orderNo(now));
        order.setUserId(userId);
        order.setMatchId(zone.getMatchId());
        order.setMatchZoneId(zone.getMatchZoneId());
        order.setTicketCount(request.ticketCount());
        order.setTotalAmount(zone.getTicketPrice().multiply(BigDecimal.valueOf(request.ticketCount())));
        order.setExpireTime(expire);
        order.setCreatedAt(now);
        orderMapper.insert(order);
        SeatAllocationResponse seats = seatAllocateService.selectAndLockSeats(zone.getMatchZoneId(),
                request.ticketCount(), order.getOrderId(), now, expire);
        for (int i = 0; i < seats.inventoryIds().size(); i++) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setInventoryId(seats.inventoryIds().get(i));
            item.setTicketPrice(zone.getTicketPrice());
            item.setZoneNameSnapshot(zone.getZoneNameSnapshot());
            item.setRowNoSnapshot(seats.rowLabel());
            item.setSeatNoSnapshot(seats.seatLabels().get(i));
            item.setItemStatus("LOCKED");
            item.setPassengerNameSnapshot(passengers.get(i).getPassengerName());
            item.setPassengerIdCardSnapshot(passengers.get(i).getIdCardNo());
            if (itemMapper.insert(item) != 1)
                throw new BusinessException(HttpStatus.CONFLICT, "failed to create complete order items");
        }
        verifyLockedOrder(order.getOrderId(), request.ticketCount());
        return detail(order.getOrderId());
    }

    public PageResponse<OrderSummaryResponse> listOwned(Long userId, OrderQueryRequest query) {
        String status = normalizeStatus(query.getOrderStatus());
        long total = orderMapper.countOwned(userId, status);
        List<OrderSummaryResponse> records = orderMapper
                .findOwnedPage(userId, status, (long) (query.page() - 1) * query.size(), query.size()).stream()
                .map(this::summary).toList();
        return new PageResponse<>(records, total, query.page(), query.size());
    }

    public OrderDetailResponse detailOwned(Long userId, Long orderId) {
        TicketOrder order = requireDetail(orderId);
        requireOwner(order, userId);
        return detail(orderId);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOwned(Long userId, Long orderId) {
        TicketOrder order = requireLocked(orderId);
        requireOwner(order, userId);
        if ("CANCELLED".equals(order.getOrderStatus()))
            return detail(orderId);
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus()))
            throw new BusinessException(HttpStatus.CONFLICT, "order status cannot be cancelled");
        LocalDateTime now = systemTimeService.now();
        cancelLockedOrder(order, !order.getExpireTime().isAfter(now) ? "PAYMENT_TIMEOUT" : "USER_CANCELLED", now);
        return detail(orderId);
    }

    @Override
    @Transactional
    public boolean closeExpiredOrder(Long orderId) {
        TicketOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null || !"PENDING_PAYMENT".equals(order.getOrderStatus()))
            return false;
        LocalDateTime now = systemTimeService.now();
        if (order.getExpireTime().isAfter(now))
            return false;
        cancelLockedOrder(order, "PAYMENT_TIMEOUT", now);
        return true;
    }

    public int closeExpiredBatch() {
        int closed = 0;
        OrderService proxy = orderServiceProvider.getObject();
        for (Long id : orderMapper.findExpiredIds(systemTimeService.now(), 100))
            if (proxy.closeExpiredOrder(id))
                closed++;
        return closed;
    }

    private void cancelLockedOrder(TicketOrder order, String reason, LocalDateTime now) {
        if (orderMapper.cancelPending(order.getOrderId(), reason, now) != 1)
            return;
        paymentMapper.closeCreated(order.getOrderId());
        int items = itemMapper.cancelLocked(order.getOrderId());
        int released = inventoryMapper.releaseLockedByOrder(order.getOrderId());
        if (items != order.getTicketCount() || released != order.getTicketCount())
            throw new BusinessException(HttpStatus.CONFLICT,
                    "order lock data is inconsistent; cancellation rolled back");
    }

    private void verifyLockedOrder(Long orderId, int expected) {
        if (itemMapper.countByOrder(orderId) != expected || inventoryMapper.countLockedByOrder(orderId) != expected)
            throw new BusinessException(HttpStatus.CONFLICT, "order lock data is incomplete");
    }

    private TicketOrder requireLocked(Long id) {
        TicketOrder o = orderMapper.findByIdForUpdate(id);
        if (o == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "order not found");
        return o;
    }

    private TicketOrder requireDetail(Long id) {
        TicketOrder o = orderMapper.findDetail(id);
        if (o == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "order not found");
        return o;
    }

    private void requireOwner(TicketOrder order, Long userId) {
        if (!order.getUserId().equals(userId))
            throw new BusinessException(HttpStatus.FORBIDDEN, "cannot access another user's order");
    }

    private OrderDetailResponse detail(Long id) {
        TicketOrder order = requireDetail(id);
        List<OrderItemResponse> items = itemMapper.findByOrder(id).stream()
                .map(i -> new OrderItemResponse(i.getItemId(), i.getInventoryId(), i.getRowNoSnapshot(),
                        i.getSeatNoSnapshot(), i.getPassengerNameSnapshot(), i.getPassengerIdCardSnapshot(),
                        i.getTicketPrice(), i.getItemStatus()))
                .toList();
        PaymentRecord p = paymentMapper.findSuccessByOrder(id);
        PaymentSummaryResponse payment = p == null ? null
                : new PaymentSummaryResponse(p.getPaymentId(), p.getPaymentNo(), p.getOrderId(), p.getPayAmount(),
                        p.getPayMethod(), p.getPayStatus(), p.getThirdPartyTradeNo(), p.getPayTime(), p.getCreatedAt());
        List<ETicketResponse> tickets = ticketMapper.findByOrder(id).stream().map(ETicketServiceImpl::response)
                .toList();
        RefundApply r = refundMapper.findByOrder(id);
        RefundResponse refund = r == null ? null
                : new RefundResponse(r.getRefundId(), r.getRefundNo(), r.getOrderId(), r.getOrderNo(),
                        r.getApplicantId(), r.getUsername(), r.getRefundAmount(), r.getRefundRate(), r.getFeeAmount(),
                        r.getProcessingMode(), r.getReason(), r.getRefundStatus(), r.getCreatedAt(), r.getAuditorId(),
                        r.getAuditTime(), r.getAuditRemark(), r.getMatchId(), r.getHomeClubName(), r.getAwayClubName(),
                        r.getMatchTime(), r.getStadiumName(), r.getZoneName(), tickets);
        return new OrderDetailResponse(summary(order), items, payment, tickets, refund);
    }

    private OrderSummaryResponse summary(TicketOrder o) {
        return new OrderSummaryResponse(o.getOrderId(), o.getOrderNo(), o.getMatchId(), o.getMatchZoneId(),
                o.getHomeClubName(), o.getAwayClubName(), o.getMatchTime(), o.getStadiumName(), o.getZoneName(),
                o.getTicketCount(), o.getTotalAmount(), o.getOrderStatus(), o.getExpireTime(), o.getPaidAt(),
                o.getCancelledAt(), o.getCancelReason(), o.getCreatedAt());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank())
            return null;
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!QUERY_STATUSES.contains(value))
            throw new BusinessException("invalid orderStatus");
        return value;
    }

    private int configInt(String key, int fallback) {
        String v = configMapper.findEnabledValue(key);
        try {
            return v == null ? fallback : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String orderNo(LocalDateTime now) {
        return "LT" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
