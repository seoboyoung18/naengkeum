package com.fridgefamer.dto.response.fridge;

/**
 * 냉장고 보관 위치별 개수 요약 — 목록/대시보드 응답에 공통 포함.
 *
 * <pre>
 * { "fridgeCount":8, "freezerCount":3, "roomTempCount":2 }
 * </pre>
 */
public record FridgeSummary(
        int fridgeCount,
        int freezerCount,
        int roomTempCount
) {}
