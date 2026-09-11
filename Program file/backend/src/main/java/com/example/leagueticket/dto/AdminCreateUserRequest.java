package com.example.leagueticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "must be an 11-digit mobile number") String phone,
        @NotBlank @Size(max = 80) String realName,
        @NotBlank String roleCode,
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "请输入4位工号数字") String employeeNo
) {
}
