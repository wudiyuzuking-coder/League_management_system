package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManagementActivationRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请输入11位手机号") String phone,
        @NotBlank String roleCode,
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "请输入4位工号数字") String employeeNo,
        @NotBlank @Size(max = 80) String realName,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank String confirmPassword
) {}
