package com.fridgefamer.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/ai/coaching 요청 Body — API 명세 §9 F20.
 *
 * <pre>{ "fridgeItemId": 1, "ingredientName": "계란" }</pre>
 *
 * <p>fridgeItemId는 선택(특정 냉장고 항목 코칭), ingredientName은 필수
 * (코칭 대상 재료명). 명세상 fridgeItemId 없이 재료명만으로도 코칭 가능.</p>
 */
public record CoachingRequest(
        Long fridgeItemId,

        @NotBlank(message = "재료명은 필수입니다")
        @Size(max = 50, message = "재료명은 50자 이하여야 합니다")
        String ingredientName
) {}