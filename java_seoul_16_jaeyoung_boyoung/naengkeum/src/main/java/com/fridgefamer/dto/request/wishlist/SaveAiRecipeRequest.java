package com.fridgefamer.dto.request.wishlist;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * AI 레시피 찜 저장 요청 — API 명세 §6 F19. POST /api/wishlist/ai.
 *
 * <p>ingredientsJson/stepsJson은 LLM이 생성한 JSON 배열을 그대로 보존한다.
 * 서비스에서 문자열로 직렬화해 ai_recipe의 JSON 컬럼에 저장하며,
 * DB CHECK(JSON_TYPE = ARRAY)로 배열만 허용된다.</p>
 */
public record SaveAiRecipeRequest(
        @NotBlank(message = "title은 비어 있을 수 없습니다")
        String title,

        String summary,

        @NotEmpty(message = "ingredientsJson은 비어 있을 수 없습니다")
        List<JsonNode> ingredientsJson,

        @NotEmpty(message = "stepsJson은 비어 있을 수 없습니다")
        List<JsonNode> stepsJson,

        @Positive(message = "cookTime은 양수여야 합니다")
        Integer cookTime
) {}
