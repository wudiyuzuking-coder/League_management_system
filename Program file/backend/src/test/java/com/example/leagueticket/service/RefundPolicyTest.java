package com.example.leagueticket.service;
import org.junit.jupiter.api.Test;import java.time.LocalDateTime;import static org.assertj.core.api.Assertions.assertThat;
class RefundPolicyTest {@Test void exactSevenDaysIsFullAndOneSecondLaterIsHalf(){LocalDateTime match=LocalDateTime.of(2030,1,8,20,0);assertThat(RefundPolicy.rate(LocalDateTime.of(2030,1,1,20,0),match)).isEqualByComparingTo("1.00");assertThat(RefundPolicy.rate(LocalDateTime.of(2030,1,1,20,0,1),match)).isEqualByComparingTo("0.50");}}
