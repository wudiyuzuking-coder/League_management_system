package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.PrefilledPassengerRequest;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.TicketPassengerService;
import com.example.leagueticket.vo.PrefilledPassengerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class TicketPassengerServiceImpl implements TicketPassengerService {
    private final TicketPassengerMapper mapper;
    private final SysUserMapper userMapper;

    @Override public List<PrefilledPassengerResponse> list(Long userId){return mapper.findByUser(userId).stream().map(PrefilledPassengerResponse::from).toList();}

    @Override @Transactional
    public PrefilledPassengerResponse add(Long userId,PrefilledPassengerRequest request){
        lockUser(userId);if(mapper.countByUser(userId)>=4)throw new BusinessException(HttpStatus.CONFLICT,"每个用户最多维护4名预填购票人");
        String idCard=normalizeCard(request.idCardNo());mapper.ensureIdentity(idCard);TicketPassengerIdentity identity=mapper.findIdentityForUpdate(idCard);
        if(identity==null)throw new BusinessException(HttpStatus.CONFLICT,"购票人身份创建失败");
        if(mapper.countUserIdentity(userId,identity.getPassengerIdentityId())>0)throw new BusinessException(HttpStatus.CONFLICT,"同一用户不能重复保存同一身份证号");
        if(mapper.countByIdentity(identity.getPassengerIdentityId())>=4)throw new BusinessException(HttpStatus.CONFLICT,"同一购票人最多被4个不同用户预填");
        UserPrefilledPassenger value=new UserPrefilledPassenger();value.setUserId(userId);value.setPassengerIdentityId(identity.getPassengerIdentityId());value.setPassengerName(request.passengerName().trim());mapper.insert(value);
        return mapper.findByUser(userId).stream().filter(v->v.getPrefilledPassengerId().equals(value.getPrefilledPassengerId())).findFirst().map(PrefilledPassengerResponse::from).orElseThrow();
    }

    @Override @Transactional
    public void delete(Long userId,Long id){lockUser(userId);UserPrefilledPassenger value=mapper.findOwnedForUpdate(id,userId);if(value==null)throw new BusinessException(HttpStatus.NOT_FOUND,"预填购票人不存在");mapper.findIdentityByIdForUpdate(value.getPassengerIdentityId());if(mapper.deleteOwned(id,userId)!=1)throw new BusinessException(HttpStatus.CONFLICT,"删除预填购票人失败");}

    @Override @Transactional
    public List<UserPrefilledPassenger> requireForOrder(Long userId,List<Long> ids,int ticketCount){
        lockUser(userId);if(ids==null||ids.size()!=ticketCount)throw new BusinessException("购票人数量必须与购票张数完全一致");
        if(new HashSet<>(ids).size()!=ids.size())throw new BusinessException("同一购票人不能在一个订单中重复选择");
        List<UserPrefilledPassenger> result=new ArrayList<>();for(Long id:ids){UserPrefilledPassenger value=mapper.findOwnedForUpdate(id,userId);if(value==null)throw new BusinessException(HttpStatus.FORBIDDEN,"只能选择当前用户维护的购票人");result.add(value);}return result;
    }

    private void lockUser(Long userId){SysUser user=userMapper.findByIdForUpdate(userId);if(user==null||!"USER".equals(user.getRoleCode()))throw new BusinessException(HttpStatus.FORBIDDEN,"仅普通用户可维护预填购票人");}
    private String normalizeCard(String value){return value.trim().toUpperCase(Locale.ROOT);}
}
