package com.fridgefamer.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /api/review 요청 Body — API 명세 §5 F06.
 *
 * <pre>{ "recipeId": 1, "rating": 5, "content": "맛있어요!" }</pre>
 *
 * <p>rating은 chk_review_rating(1~5) 제약에 맞춰 1~5 강제.
 * 한 회원이 같은 레시피에 중복 작성 시 DB UNIQUE(uq_review_member_recipe) → 409.</p>
 */
public record CreateReviewRequest(
        @NotNull(message = "recipeId는 필수입니다")
        @Positive(message = "recipeId는 양수여야 합니다")
        Long recipeId,

        @NotNull(message = "평점은 필수입니다")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다")
        @Max(value = 5, message = "평점은 5 이하여야 합니다")
        Integer rating,

        @NotBlank(message = "리뷰 내용은 필수입니다")
        @Size(max = 1000, message = "리뷰는 1000자 이하여야 합니다")
        String content
) {}