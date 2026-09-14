package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubInfoMapper;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.service.SysRolePermissionService;
import com.example.leagueticket.service.SysRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCancellationServiceImplTest {

    @Mock SysUserMapper userMapper;
    @Mock ClubInfoMapper clubMapper;
    @Mock SysRoleService roleService;
    @Mock SysRolePermissionService rolePermissionService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks SysUserServiceImpl service;

    @Test void enabledUserIsSoftCancelled() {
        SysUser user=user(1L,"USER","ENABLED",null);
        when(userMapper.findByIdForUpdate(1L)).thenReturn(user);
        when(userMapper.cancelEnabled(1L)).thenReturn(1);
        service.cancelAccount(1L,"USER");
        verify(userMapper).cancelEnabled(1L);
    }

    @Test void cancelledAccountIsExplicitlyRejected() {
        when(userMapper.findByIdForUpdate(1L)).thenReturn(user(1L,"USER","CANCELLED",null));
        assertThatThrownBy(()->service.cancelAccount(1L,"USER"))
                .isInstanceOf(BusinessException.class).hasMessage("该账号已注销");
        verify(userMapper,never()).cancelEnabled(anyLong());
    }

    @Test void cancelledAccountCannotBeReenabledByAdminStatusUpdate() {
        when(userMapper.findByIdForUpdate(1L)).thenReturn(user(1L,"USER","CANCELLED",null));
        assertThatThrownBy(()->service.updateStatus(1L,"ENABLED"))
                .isInstanceOf(BusinessException.class).hasMessage("已注销账号不能重新启用或停用");
        verify(userMapper,never()).updateStatus(anyLong(),anyString());
    }

    @Test void disabledAndLockedAccountsCannotChangeStateThroughCancellation() {
        when(userMapper.findByIdForUpdate(1L)).thenReturn(user(1L,"USER","DISABLED",null));
        assertThatThrownBy(()->service.cancelAccount(1L,"USER"))
                .isInstanceOf(BusinessException.class).hasMessage("当前账号状态不允许注销");
        reset(userMapper);
        when(userMapper.findByIdForUpdate(2L)).thenReturn(user(2L,"USER","LOCKED",null));
        assertThatThrownBy(()->service.cancelAccount(2L,"USER"))
                .isInstanceOf(BusinessException.class).hasMessage("当前账号状态不允许注销");
    }

    @Test void boundClubLeaderCannotCancel() {
        when(userMapper.findByIdForUpdate(2L)).thenReturn(user(2L,"CLUB","ENABLED",9L));
        assertThatThrownBy(()->service.cancelAccount(2L,"CLUB"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前账号仍为俱乐部负责人，无法直接注销，请联系系统管理员处理俱乐部负责人关系");
        verify(userMapper,never()).cancelEnabled(anyLong());
    }

    @Test void eventAdminIsCancelledWithoutDeletingHistoricalObjects() {
        when(userMapper.findByIdForUpdate(3L)).thenReturn(user(3L,"EVENT_ADMIN","ENABLED",null));
        when(userMapper.cancelEnabled(3L)).thenReturn(1);
        service.cancelAccount(3L,"EVENT_ADMIN");
        verify(userMapper).cancelEnabled(3L);
        verifyNoInteractions(clubMapper);
    }

    @Test void nonLastAdminCanCancelAfterGlobalAdminLock() {
        when(userMapper.lockRoleByCode("ADMIN")).thenReturn(4L);
        when(userMapper.findByIdForUpdate(4L)).thenReturn(user(4L,"ADMIN","ENABLED",null));
        when(userMapper.countEnabledByRole("ADMIN")).thenReturn(2);
        when(userMapper.cancelEnabled(4L)).thenReturn(1);
        service.cancelAccount(4L,"ADMIN");
        var order=inOrder(userMapper);
        order.verify(userMapper).lockRoleByCode("ADMIN");
        order.verify(userMapper).findByIdForUpdate(4L);
        order.verify(userMapper).countEnabledByRole("ADMIN");
        order.verify(userMapper).cancelEnabled(4L);
    }

    @Test void lastEnabledAdminIsRejected() {
        when(userMapper.lockRoleByCode("ADMIN")).thenReturn(4L);
        when(userMapper.findByIdForUpdate(4L)).thenReturn(user(4L,"ADMIN","ENABLED",null));
        when(userMapper.countEnabledByRole("ADMIN")).thenReturn(1);
        assertThatThrownBy(()->service.cancelAccount(4L,"ADMIN"))
                .isInstanceOf(BusinessException.class).hasMessage("当前账号是最后一个可用系统管理员，无法注销");
        verify(userMapper,never()).cancelEnabled(anyLong());
    }

    @Test void failedConditionalUpdateIsReportedAndTransactionCanRollBack() {
        when(userMapper.findByIdForUpdate(5L)).thenReturn(user(5L,"USER","ENABLED",null));
        when(userMapper.cancelEnabled(5L)).thenThrow(new DataAccessResourceFailureException("simulated"));
        assertThatThrownBy(()->service.cancelAccount(5L,"USER"))
                .isInstanceOf(DataAccessResourceFailureException.class).hasMessage("simulated");
    }

    private SysUser user(Long id,String role,String status,Long clubId){
        SysUser user=new SysUser();user.setUserId(id);user.setRoleCode(role);user.setUserStatus(status);user.setClubId(clubId);return user;
    }
}
