package com.fridgefamer.dto.request.recipe;

import jakarta.validation.constraints.Size;

/**
 * 작성자 후기 저장 요청 — PATCH /api/recipe/{recipeId}/review.
 *
 * <p>본인이 등록한 레시피 상세에서 "내 후기"를 작성/수정한다. 빈 문자열/null이면 후기 삭제로 간주.</p>
 */
public record AuthorReviewRequest(
        @Size(max = 1000, message = "후기는 1000자 이하여야 합니다")
        String review
) {}
