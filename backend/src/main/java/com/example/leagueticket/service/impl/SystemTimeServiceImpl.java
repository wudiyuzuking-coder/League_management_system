package com.example.leagueticket.service.impl;

import com.example.leagueticket.mapper.OperationLogMapper;
import com.example.leagueticket.mapper.SystemConfigMapper;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.SystemTimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class SystemTimeServiceImpl implements SystemTimeService {
    static final String OFFSET_KEY="SYSTEM_TIME_OFFSET_SECONDS";
    private static final String DESCRIPTION="课程演示系统时间相对服务器真实时间的偏移秒数";
    private static final DateTimeFormatter LOG_TIME=DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final SystemConfigMapper configMapper;
    private final OperationLogMapper operationLogMapper;
    private final Clock systemClock;

    @Override public LocalDateTime realNow(){return LocalDateTime.now(systemClock);}
    @Override public LocalDateTime now(){LocalDateTime real=realNow();return applyOffset(real,readOffset());}
    @Override public LocalDateTime getCurrentSystemTime(){return now();}
    @Override public SystemTimeResponse getTime(){LocalDateTime real=realNow();long offset=readOffset();return new SystemTimeResponse(applyOffset(real,offset),real,offset);}

    @Override @Transactional
    public SystemTimeResponse setCurrentSystemTime(LocalDateTime targetTime,AuthenticatedUser operator){
        long beforeOffset=lockOffset();
        LocalDateTime real=realNow();
        LocalDateTime before=applyOffset(real,beforeOffset);
        long afterOffset=Duration.between(real,targetTime).getSeconds();
        updateAndLog(operator,"SET","PUT","/api/system-time",real,before,targetTime,beforeOffset,afterOffset);
        return new SystemTimeResponse(applyOffset(real,afterOffset),real,afterOffset);
    }

    @Override @Transactional
    public SystemTimeResponse resetToRealTime(AuthenticatedUser operator){
        long beforeOffset=lockOffset();
        LocalDateTime real=realNow();
        LocalDateTime before=applyOffset(real,beforeOffset);
        updateAndLog(operator,"RESET","POST","/api/system-time/reset",real,before,real,beforeOffset,0);
        return new SystemTimeResponse(real,real,0);
    }

    private void updateAndLog(AuthenticatedUser operator,String type,String method,String uri,LocalDateTime real,
                              LocalDateTime before,LocalDateTime after,long beforeOffset,long afterOffset){
        if(configMapper.updateOffset(OFFSET_KEY,Long.toString(afterOffset),DESCRIPTION)!=1)
            throw new IllegalStateException("system time offset configuration could not be updated");
        String detail="username="+operator.username()+", role="+operator.roleCode()+
                ", beforeSystemTime="+LOG_TIME.format(before)+", afterSystemTime="+LOG_TIME.format(after)+
                ", offsetSeconds="+beforeOffset+" -> "+afterOffset+", realTime="+LOG_TIME.format(real);
        if(operationLogMapper.insertSystemTimeLog(operator.userId(),type,method,uri,detail,real)!=1)
            throw new IllegalStateException("system time operation log could not be written");
    }

    private long readOffset(){return parseOffset(configMapper.findEnabledValue(OFFSET_KEY));}
    private long lockOffset(){String value=configMapper.findValueForUpdate(OFFSET_KEY);if(value==null){configMapper.ensureOffsetConfig(OFFSET_KEY,DESCRIPTION);value=configMapper.findValueForUpdate(OFFSET_KEY);}return parseOffset(value);}
    private long parseOffset(String value){
        if(value==null)throw new IllegalStateException("enabled system time offset configuration is missing");
        try{return Long.parseLong(value);}catch(NumberFormatException e){throw new IllegalStateException("invalid system time offset configuration: "+value,e);}
    }
    private LocalDateTime applyOffset(LocalDateTime real,long offset){
        try{return real.plusSeconds(offset);}catch(DateTimeException e){throw new IllegalStateException("system time offset is outside the supported range",e);}
    }
}
