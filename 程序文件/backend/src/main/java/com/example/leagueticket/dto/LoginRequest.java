package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入11位手机号") String phone,
        @NotBlank(message = "请输入密码") String password,
        @NotBlank(message = "请选择身份") @Size(max = 32) String roleCode,
        @Size(max = 16) String employeeNo
) {
}
