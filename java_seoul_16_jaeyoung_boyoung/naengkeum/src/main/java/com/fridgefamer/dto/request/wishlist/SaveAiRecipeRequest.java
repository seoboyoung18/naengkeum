package com.fridgefamer.dto.request.wishlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/wishlist/ai 요청 Body — API 명세 §6 F19.
 *
 * <pre>
 * { "title":"두부 계란 볶음", "summary":"...", "ingredientsJson":["두부 1모","계란 2개"],
 *   "stepsJson":["두부를 깍둑썬다","계란을 푼다"], "cookTime":15 }
 * </pre>
 *
 * <p>ingredientsJson/stepsJson은 문자열 배열. ai_recipe 테이블의 JSON 컬럼에
 * 배열로 저장되며, JSON_TYPE=ARRAY 제약을 만족해야 한다(비어있으면 안 됨).</p>
 */
public record SaveAiRecipeRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        String title,

        @Size(max = 2000, message = "소개는 2000자 이하여야 합니다")
        String summary,

        @NotEmpty(message = "재료 목록은 비어있을 수 없습니다")
        List<String> ingredientsJson,

        @NotEmpty(message = "조리 순서는 비어있을 수 없습니다")
        List<String> stepsJson,

        @Positive(message = "조리 시간은 양수여야 합니다")
        Integer cookTime
) {}