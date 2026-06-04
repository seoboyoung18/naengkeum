package com.fridgefamer.dto.request.ai;

/**
 * AI 레시피 추천 요청 — API 명세 §9 F19. POST /api/ai/recommend.
 *
 * <pre>{ "prioritizeExpiry": true, "useAllFridge": false, "applyAllergy": true }</pre>
 *
 * <p>모두 선택값(미지정 시 false 취급). 접근자에 null-safe 기본값을 둔다.</p>
 */
public record AiRecommendRequest(
        Boolean prioritizeExpiry,
        Boolean useAllFridge,
        Boolean applyAllergy
) {
    public boolean prioritizeExpiryOrDefault() { return Boolean.TRUE.equals(prioritizeExpiry); }
    public boolean useAllFridgeOrDefault()     { return Boolean.TRUE.equals(useAllFridge); }
    public boolean applyAllergyOrDefault()     { return Boolean.TRUE.equals(applyAllergy); }
}
