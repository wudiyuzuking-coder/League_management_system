package com.example.leagueticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserQueryRequest {
    @Min(1)
    private int page = 1;
    @Min(1)
    @Max(100)
    private int size = 10;
    private String username;
    private String phone;
    private String roleCode;
    private String userStatus;
}
