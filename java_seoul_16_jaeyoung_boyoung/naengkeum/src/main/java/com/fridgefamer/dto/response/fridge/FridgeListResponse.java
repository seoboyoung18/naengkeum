package com.fridgefamer.dto.response.fridge;

import java.util.List;

/**
 * GET /api/fridge 200 응답 — 재료 목록 + 보관 위치별 요약.
 *
 * <pre>
 * { "items": [ {FridgeItem}, ... ],
 *   "summary": { "fridgeCount":8, "freezerCount":3, "roomTempCount":2 } }
 * </pre>
 */
public record FridgeListResponse(
        List<FridgeItem> items,
        FridgeSummary summary
) {}
