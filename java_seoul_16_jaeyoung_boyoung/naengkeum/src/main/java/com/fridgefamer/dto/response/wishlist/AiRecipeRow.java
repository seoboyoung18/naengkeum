package com.fridgefamer.dto.response.wishlist;

import java.time.LocalDateTime;

/**
 * ai_recipe 단건 조회 내부 행 — JSON 컬럼은 문자열로 읽어 서비스에서 파싱한다.
 */
public record AiRecipeRow(
        Long aiRecipeId,
        Long memberId,
        String title,
        String summary,
        String ingredientsJson,
        String stepsJson,
        Integer cookTime,
        LocalDateTime createdAt
) {}
