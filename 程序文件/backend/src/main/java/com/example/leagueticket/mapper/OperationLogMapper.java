package com.example.leagueticket.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface OperationLogMapper {
    @Insert("""
        INSERT INTO operation_log(operator_id,module_name,operation_type,request_method,request_uri,
          operation_description,result_status,created_at)
        VALUES(#{operatorId},'SYSTEM_TIME',#{operationType},#{requestMethod},#{requestUri},
          #{description},'SUCCESS',#{realTime})
        """)
    int insertSystemTimeLog(@Param("operatorId")Long operatorId,
                            @Param("operationType")String operationType,
                            @Param("requestMethod")String requestMethod,
                            @Param("requestUri")String requestUri,
                            @Param("description")String description,
                            @Param("realTime")LocalDateTime realTime);
}
