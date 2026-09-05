package com.example.leagueticket.dto;

import jakarta.validation.constraints.Size;

public record RefundAuditRequest(@Size(max=500,message="auditReason must not exceed 500 characters") String auditReason) {}
