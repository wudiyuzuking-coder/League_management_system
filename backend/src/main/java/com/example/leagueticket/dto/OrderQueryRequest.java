package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OrderQueryRequest {
    private String orderStatus;
    @Min(1) private Integer page=1;
    @Min(1) @Max(100) private Integer size=10;
    public int page(){return page==null?1:page;}
    public int size(){return size==null?10:size;}
}
