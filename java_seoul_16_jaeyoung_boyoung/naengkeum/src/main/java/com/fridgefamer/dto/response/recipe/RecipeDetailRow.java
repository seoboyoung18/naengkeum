package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 상세 조회 내부 행(row) — 매퍼 ↔ 서비스 조립용.
 *
 * <p>recipe 본문 + 영양정보 + 집계를 한 방에 조회한다. 서비스가 ingredients/steps를
 * 추가 조회로 붙여 {@link RecipeDetail}을 완성한다. 없으면 매퍼가 null 반환.</p>
 */
public record RecipeDetailRow(
        Long recipeId,
        String title,
        String summary,
        String thumbnailUrl,
        Integer cookTime,
        Integer calories,
        BigDecimal carbs,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal sodium,
        BigDecimal avgRating,
        int reviewCount,
        boolean isWishlisted
) {}
