package com.example.leagueticket.vo;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int size) {
}
