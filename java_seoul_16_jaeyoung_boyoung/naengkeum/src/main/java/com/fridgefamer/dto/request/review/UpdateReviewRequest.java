package com.fridgefamer.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/review/{reviewId} 요청 Body — API 명세 §5 F08.
 *
 * <pre>{ "rating": 4, "content": "수정 내용" }</pre>
 *
 * <p>recipeId는 변경 불가(경로의 reviewId로 대상 식별). rating/content만 수정.</p>
 */
public record UpdateReviewRequest(
        @NotNull(message = "평점은 필수입니다")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다")
        @Max(value = 5, message = "평점은 5 이하여야 합니다")
        Integer rating,

        @NotBlank(message = "리뷰 내용은 필수입니다")
        @Size(max = 1000, message = "리뷰는 1000자 이하여야 합니다")
        String content
) {}