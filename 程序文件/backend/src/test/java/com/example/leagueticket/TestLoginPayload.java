package com.example.leagueticket;

import java.util.LinkedHashMap;
import java.util.Map;

final class TestLoginPayload {
    private TestLoginPayload() {
    }

    static Map<String, String> forPhone(String phone, String password) {
        return forRole(phone, password, roleFor(phone), employeeNoFor(phone));
    }

    static Map<String, String> forRole(String phone, String password, String roleCode, String employeeNo) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("phone", phone);
        payload.put("password", password);
        payload.put("roleCode", roleCode);
        if (employeeNo != null) {
            payload.put("employeeNo", employeeNo);
        }
        return payload;
    }

    private static String roleFor(String phone) {
        return switch (phone) {
            case "13800000002" -> "ADMIN";
            case "13800000003", "13900003010", "13917000051", "13917000052" -> "CLUB";
            case "13800000005" -> "EVENT_ADMIN";
            default -> "USER";
        };
    }

    private static String employeeNoFor(String phone) {
        return switch (phone) {
            case "13800000002" -> "SA0001";
            case "13800000005" -> "EA0001";
            default -> null;
        };
    }
}
