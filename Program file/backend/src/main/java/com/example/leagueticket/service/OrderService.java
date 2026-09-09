package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.vo.*;

public interface OrderService {
    OrderDetailResponse create(Long userId,OrderCreateRequest request);
    PageResponse<OrderSummaryResponse> listOwned(Long userId,OrderQueryRequest query);
    OrderDetailResponse detailOwned(Long userId,Long orderId);
    OrderDetailResponse cancelOwned(Long userId,Long orderId);
    boolean closeExpiredOrder(Long orderId);
    int closeExpiredBatch();
}
