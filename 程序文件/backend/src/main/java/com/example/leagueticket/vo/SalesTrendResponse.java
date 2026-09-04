package com.example.leagueticket.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesTrendResponse {
    private LocalDate statDate;
    private BigDecimal grossSalesAmount;
    private BigDecimal refundAmount;
    private BigDecimal netSalesAmount;
    private long ticketsSold;
}
