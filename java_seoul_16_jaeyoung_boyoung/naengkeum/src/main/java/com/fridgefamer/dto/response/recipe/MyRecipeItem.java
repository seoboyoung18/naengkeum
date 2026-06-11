package com.fridgefamer.dto.response.recipe;

import java.time.LocalDateTime;

/**
 * 마이 레시피 목록 단건 — GET /api/recipe/mine.
 *
 * <p>"내 레시피로 담기"로 만든 내 레시피(author_id = 나). 공개/비공개 모두 포함하며,
 * isPublic으로 마이페이지에서 "공개됨 / 비공개 + 공개하기"를 구분한다.</p>
 */
public record MyRecipeItem(
        Long recipeId,
        String title,
        Integer cookTime,
        boolean isPublic,
        String source,
        LocalDateTime createdAt
) {}
