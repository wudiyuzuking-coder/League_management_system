package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrefilledPassengerRequest(
        @NotBlank @Size(max=80) String passengerName,
        @NotBlank @Pattern(regexp="^\\d{17}[\\dXx]$",message="身份证号格式不正确") String idCardNo) {}
