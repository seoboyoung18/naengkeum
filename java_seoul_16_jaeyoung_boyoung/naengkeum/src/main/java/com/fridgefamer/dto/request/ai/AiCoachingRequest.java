package com.fridgefamer.dto.request.ai;

/**
 * AI 식재료 코칭 요청 — API 명세 §9 F20. POST /api/ai/coaching.
 *
 * <pre>{ "fridgeItemId": 1, "ingredientName": "계란" }</pre>
 *
 * <p>ingredientName은 코칭 대상 재료명(필수). fridgeItemId는 특정 냉장고 항목을
 * 가리키는 선택값(없어도 재료명만으로 코칭 가능). 빈 재료명은 서비스에서 400으로 차단.</p>
 */
public record AiCoachingRequest(
        Long fridgeItemId,
        String ingredientName
) {
    public boolean hasIngredientName() {
        return ingredientName != null && !ingredientName.isBlank();
    }
}