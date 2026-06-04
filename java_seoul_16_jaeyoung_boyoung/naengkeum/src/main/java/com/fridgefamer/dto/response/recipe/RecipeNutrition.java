package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 영양정보 — 식약처 COOKRCP01 제공값(완성 요리 1인분 기준).
 *
 * <p>모든 값은 미적재 레시피에서 NULL 가능하므로 박싱 타입으로 매핑한다.</p>
 */
public record RecipeNutrition(
        Integer calories,
        BigDecimal carbs,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal sodium
) {}
