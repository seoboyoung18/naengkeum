package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 목록 조회 내부 행(row) — 매퍼 ↔ 서비스 조립용.
 *
 * <p>목록 쿼리는 recipe 본문 + 집계(avgRating/reviewCount/isWishlisted)만 한 방에 가져오고,
 * mainIngredients는 별도 일괄 조회로 채워 N+1을 피한다. 서비스가 이 row를
 * {@link RecipeListItem}으로 변환한다.</p>
 */
public record RecipeListRow(
        Long recipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        int reviewCount,
        boolean isWishlisted,
        String source
) {}
