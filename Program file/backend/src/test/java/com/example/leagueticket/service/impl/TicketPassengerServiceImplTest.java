package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PrefilledPassengerRequest;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.entity.TicketPassengerIdentity;
import com.example.leagueticket.entity.UserPrefilledPassenger;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.mapper.TicketPassengerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketPassengerServiceImplTest {
    @Mock TicketPassengerMapper mapper;
    @Mock SysUserMapper userMapper;
    TicketPassengerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketPassengerServiceImpl(mapper, userMapper);
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setRoleCode("USER");
        when(userMapper.findByIdForUpdate(1L)).thenReturn(user);
    }

    @Test
    void rejectsFifthPassengerBeforeCreatingIdentity() {
        when(mapper.countByUser(1L)).thenReturn(4);
        assertThatThrownBy(() -> service.add(1L, request("张三", "110101199001010011")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("每个用户最多维护4名预填购票人");
        verify(mapper, never()).ensureIdentity(anyString());
    }

    @Test
    void rejectsDuplicateAndGlobalFifthAssociation() {
        TicketPassengerIdentity identity = identity(8L);
        when(mapper.findIdentityForUpdate("110101199001010011")).thenReturn(identity);

        when(mapper.countUserIdentity(1L, 8L)).thenReturn(1);
        assertThatThrownBy(() -> service.add(1L, request("张三", "110101199001010011")))
                .hasMessage("同一用户不能重复保存同一身份证号");

        when(mapper.countUserIdentity(1L, 8L)).thenReturn(0);
        when(mapper.countByIdentity(8L)).thenReturn(4);
        assertThatThrownBy(() -> service.add(1L, request("张三", "110101199001010011")))
                .hasMessage("同一购票人最多被4个不同用户预填");
        verify(mapper, never()).insert(any());
    }

    @Test
    void orderRequiresExactDistinctOwnedPassengersAndPreservesSelectionOrder() {
        UserPrefilledPassenger first = passenger(10L);
        UserPrefilledPassenger second = passenger(11L);
        when(mapper.findOwnedForUpdate(10L, 1L)).thenReturn(first);
        when(mapper.findOwnedForUpdate(11L, 1L)).thenReturn(second);

        assertThat(service.requireForOrder(1L, List.of(11L, 10L), 2)).containsExactly(second, first);
        assertThatThrownBy(() -> service.requireForOrder(1L, List.of(10L), 2))
                .hasMessage("购票人数量必须与购票张数完全一致");
        assertThatThrownBy(() -> service.requireForOrder(1L, List.of(10L, 10L), 2))
                .hasMessage("同一购票人不能在一个订单中重复选择");
    }

    @Test
    void deleteIsScopedToOwnerAndReleasesAssociation() {
        UserPrefilledPassenger value = passenger(10L);
        value.setPassengerIdentityId(8L);
        when(mapper.findOwnedForUpdate(10L, 1L)).thenReturn(value);
        when(mapper.deleteOwned(10L, 1L)).thenReturn(1);

        service.delete(1L, 10L);

        verify(mapper).findIdentityByIdForUpdate(8L);
        verify(mapper).deleteOwned(10L, 1L);
    }

    private PrefilledPassengerRequest request(String name, String card) {
        return new PrefilledPassengerRequest(name, card);
    }

    private TicketPassengerIdentity identity(long id) {
        TicketPassengerIdentity value = new TicketPassengerIdentity();
        value.setPassengerIdentityId(id);
        return value;
    }

    private UserPrefilledPassenger passenger(long id) {
        UserPrefilledPassenger value = new UserPrefilledPassenger();
        value.setPrefilledPassengerId(id);
        return value;
    }
}
