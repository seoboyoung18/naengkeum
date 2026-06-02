package com.fridgefamer.dto.response.fridge;

import java.util.List;

/**
 * GET /api/fridge/dashboard 200 응답 (F17) — 냉장고 요약 + D-3 이내 임박 목록.
 *
 * <p>expiringItems는 dDay &lt;= 3 인 재료를 임박순(expiry 오름차순)으로 담는다.
 * 이미 지난(dDay 음수) 재료도 포함한다 — 사용자에게 즉시 알려야 하므로.</p>
 */
public record FridgeDashboardResponse(
        FridgeSummary summary,
        List<FridgeItem> expiringItems
) {}
