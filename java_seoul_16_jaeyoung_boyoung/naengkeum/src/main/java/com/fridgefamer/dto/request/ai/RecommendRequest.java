package com.fridgefamer.dto.request.ai;

/**
 * POST /api/ai/recommend 요청 Body — API 명세 §9 F19.
 *
 * <pre>{ "prioritizeExpiry": true, "useAllFridge": false, "applyAllergy": true }</pre>
 *
 * <p>모두 선택 옵션(기본 false). null로 와도 되도록 Boolean 래퍼 사용,
 * 서비스에서 null → false로 처리.</p>
 *
 * @param prioritizeExpiry 유통기한 임박 재료 우선 사용
 * @param useAllFridge     냉장고 전체 재료 사용(false면 임박/주요 위주)
 * @param applyAllergy     회원 알레르기 정보 반영(현재는 프롬프트 힌트용)
 */
public record RecommendRequest(
        Boolean prioritizeExpiry,
        Boolean useAllFridge,
        Boolean applyAllergy
) {}