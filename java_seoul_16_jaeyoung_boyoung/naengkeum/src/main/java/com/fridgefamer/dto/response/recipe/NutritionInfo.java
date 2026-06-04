package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 영양정보 — 식약처 COOKRCP01 제공값(완성 요리 기준).
 *
 * <pre>{ "calories": 180, "carbs": 8.5, "protein": 15.2, "fat": 9.1, "sodium": 420 }</pre>
 *
 * <p>recipe 테이블의 calories/carbs/protein/fat/sodium 컬럼 매핑.
 * 공공레시피는 대부분 채워져 있고, V3 샘플 등 일부는 null일 수 있음.</p>
 */
public record NutritionInfo(
        Integer calories,
        BigDecimal carbs,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal sodium
) {}