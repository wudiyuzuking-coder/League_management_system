package com.example.leagueticket.service;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.vo.*;

public interface RefundService {
    RefundResponse apply(Long userId,Long orderId,RefundApplyRequest request);
    PageResponse<RefundResponse> listOwned(Long userId,RefundQueryRequest query);
    RefundResponse detailOwned(Long userId,Long refundId);
    PageResponse<RefundResponse> listAdmin(RefundQueryRequest query);
    RefundResponse detailAdmin(Long refundId);
    RefundResponse approve(Long adminId,Long refundId,RefundAuditRequest request);
    RefundResponse reject(Long adminId,Long refundId,RefundAuditRequest request);
}
