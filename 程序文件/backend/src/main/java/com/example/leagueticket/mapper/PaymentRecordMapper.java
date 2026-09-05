package com.example.leagueticket.mapper;

import com.example.leagueticket.entity.PaymentRecord;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface PaymentRecordMapper {
    @Insert("INSERT INTO payment_record(payment_no,order_id,pay_amount,pay_method,pay_status,created_at) VALUES(#{paymentNo},#{orderId},#{payAmount},#{payMethod},'CREATED',#{createdAt})")
    @Options(useGeneratedKeys=true,keyProperty="paymentId") int insert(PaymentRecord payment);
    @Update("UPDATE payment_record SET pay_status=#{status},third_party_trade_no=#{tradeNo},pay_time=#{payTime} WHERE payment_id=#{id} AND pay_status='CREATED'")
    int finish(@Param("id")Long id,@Param("status")String status,@Param("tradeNo")String tradeNo,@Param("payTime")LocalDateTime payTime);
    @Select("SELECT * FROM payment_record WHERE order_id=#{orderId} AND pay_status='SUCCESS' ORDER BY payment_id DESC LIMIT 1")
    PaymentRecord findSuccessByOrder(Long orderId);
    @Select("SELECT * FROM payment_record WHERE order_id=#{orderId} ORDER BY payment_id DESC LIMIT 1")
    PaymentRecord findLatestByOrder(Long orderId);
    @Update("UPDATE payment_record SET pay_status='CLOSED' WHERE order_id=#{orderId} AND pay_status='CREATED'")
    int closeCreated(Long orderId);
}
