package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TicketQueryRequest {
    private String ticketStatus;
    @Min(1) private int page = 1;
    @Min(1) @Max(100) private int size = 10;
}
