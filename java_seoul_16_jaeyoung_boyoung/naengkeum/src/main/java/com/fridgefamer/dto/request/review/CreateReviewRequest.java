package com.fridgefamer.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 리뷰 작성 요청 — API 명세 §5 F06. POST /api/review.
 *
 * <p>같은 회원이 같은 레시피에 중복 작성 시 DB UNIQUE(member_id,recipe_id) 위반 →
 * DuplicateKeyException → 409로 자동 변환된다.</p>
 */
public record CreateReviewRequest(
        @NotNull(message = "recipeId는 필수입니다")
        @Positive(message = "recipeId는 양수여야 합니다")
        Long recipeId,

        @NotNull(message = "rating은 필수입니다")
        @Min(value = 1, message = "rating은 1 이상이어야 합니다")
        @Max(value = 5, message = "rating은 5 이하여야 합니다")
        Integer rating,

        @NotBlank(message = "content는 비어 있을 수 없습니다")
        String content
) {}
