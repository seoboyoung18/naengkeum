package com.fridgefamer.dto.response.wishlist;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 저장된 AI 레시피 단건 상세 — GET /api/wishlist/ai/{aiRecipeId}.
 *
 * <p>ingredients/steps는 ai_recipe의 JSON 배열을 그대로 노출(파싱된 JsonNode → JSON 배열로 직렬화).</p>
 */
public record AiRecipeDetail(
        Long aiRecipeId,
        String title,
        String summary,
        JsonNode ingredients,
        JsonNode steps,
        Integer cookTime,
        LocalDateTime createdAt
) {}
