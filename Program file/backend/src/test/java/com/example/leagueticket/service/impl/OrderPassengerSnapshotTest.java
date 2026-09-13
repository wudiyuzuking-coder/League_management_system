package com.example.leagueticket.service.impl;

import com.example.leagueticket.algorithm.seat.SeatAllocateService;
import com.example.leagueticket.dto.OrderCreateRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.*;
import com.example.leagueticket.vo.SeatAllocationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPassengerSnapshotTest {
    @Mock TicketOrderMapper orderMapper;
    @Mock OrderItemMapper itemMapper;
    @Mock MatchTicketZoneMapper zoneMapper;
    @Mock MatchInfoMapper matchMapper;
    @Mock MatchSeatInventoryMapper inventoryMapper;
    @Mock PaymentRecordMapper paymentMapper;
    @Mock ETicketMapper ticketMapper;
    @Mock RefundApplyMapper refundMapper;
    @Mock SystemConfigMapper configMapper;
    @Mock SystemTimeService systemTimeService;
    @Mock TicketSalePolicy salePolicy;
    @Mock SeatAllocateService seatAllocateService;
    @Mock StandardTicketZoneSelector zoneSelector;
    @Mock TicketPassengerService passengerService;
    @Mock ObjectProvider<OrderService> orderServiceProvider;
    OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderMapper, itemMapper, zoneMapper, matchMapper, inventoryMapper,
                paymentMapper, ticketMapper, refundMapper, configMapper, systemTimeService, salePolicy,
                seatAllocateService, zoneSelector, passengerService, orderServiceProvider);
    }

    @Test
    void createsOneImmutablePassengerSnapshotPerTicket() {
        MatchTicketZone zone = new MatchTicketZone();
        zone.setMatchZoneId(7L);
        zone.setMatchId(3L);
        zone.setTicketPrice(new BigDecimal("120.00"));
        zone.setZoneNameSnapshot("东看台");
        MatchInfo match = new MatchInfo();
        match.setMatchId(3L);
        UserPrefilledPassenger first = passenger("张三", "110101199001010011");
        UserPrefilledPassenger second = passenger("李四", "11010119900101002X");
        List<OrderItem> inserted = new ArrayList<>();
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

        when(configMapper.findEnabledValue(anyString())).thenReturn(null);
        when(passengerService.requireForOrder(1L, List.of(10L, 11L), 2)).thenReturn(List.of(first, second));
        when(zoneMapper.findByIdForUpdate(7L)).thenReturn(zone);
        when(matchMapper.findById(3L)).thenReturn(match);
        when(systemTimeService.now()).thenReturn(now);
        when(inventoryMapper.countStatus(7L, "AVAILABLE")).thenReturn(2L);
        doAnswer(invocation -> { ((TicketOrder) invocation.getArgument(0)).setOrderId(20L); return 1; })
                .when(orderMapper).insert(any());
        when(seatAllocateService.selectAndLockSeats(eq(7L), eq(2), eq(20L), eq(now), any()))
                .thenReturn(new SeatAllocationResponse(7L, 3L, 2, 1, "1排",
                        List.of(101L, 102L), List.of(201L, 202L), List.of(1, 2), List.of("1号", "2号"), "test", "东 VIP"));
        when(itemMapper.insert(any())).thenAnswer(invocation -> { inserted.add(invocation.getArgument(0)); return 1; });
        when(itemMapper.countByOrder(20L)).thenReturn(2);
        when(inventoryMapper.countLockedByOrder(20L)).thenReturn(2);
        when(orderMapper.findDetail(20L)).thenAnswer(invocation -> {
            TicketOrder order = new TicketOrder();
            order.setOrderId(20L); order.setUserId(1L); order.setMatchId(3L); order.setMatchZoneId(7L);
            order.setTicketCount(2); order.setTotalAmount(new BigDecimal("240.00")); order.setOrderStatus("PENDING_PAYMENT");
            return order;
        });
        when(itemMapper.findByOrder(20L)).thenReturn(inserted);
        when(ticketMapper.findByOrder(20L)).thenReturn(List.of());

        service.create(1L, new OrderCreateRequest(7L, null, null, 2, List.of(10L, 11L)));

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(itemMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(OrderItem::getPassengerNameSnapshot)
                .containsExactly("张三", "李四");
        assertThat(captor.getAllValues()).extracting(OrderItem::getPassengerIdCardSnapshot)
                .containsExactly("110101199001010011", "11010119900101002X");
    }

    private UserPrefilledPassenger passenger(String name, String card) {
        UserPrefilledPassenger value = new UserPrefilledPassenger();
        value.setPassengerName(name);
        value.setIdCardNo(card);
        return value;
    }
}
