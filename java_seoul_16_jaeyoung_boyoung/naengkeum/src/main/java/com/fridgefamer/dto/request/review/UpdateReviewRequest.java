package com.fridgefamer.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 리뷰 수정 요청 — API 명세 §5 F08. PUT /api/review/{reviewId}.
 *
 * <p>recipeId는 변경 불가(본문에 없음). 본인 리뷰만 수정 가능(403 검증은 서비스).</p>
 */
public record UpdateReviewRequest(
        @NotNull(message = "rating은 필수입니다")
        @Min(value = 1, message = "rating은 1 이상이어야 합니다")
        @Max(value = 5, message = "rating은 5 이하여야 합니다")
        Integer rating,

        @NotBlank(message = "content는 비어 있을 수 없습니다")
        String content
) {}
