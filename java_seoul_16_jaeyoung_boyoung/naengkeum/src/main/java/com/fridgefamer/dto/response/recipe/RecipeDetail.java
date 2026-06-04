package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레시피 상세 응답 — API 명세 §4 GET /api/recipe/{recipeId}.
 *
 * <p>스키마-명세 갭(1번 정책)으로 항상 고정/null인 필드:
 * <ul>
 *   <li>difficulty, servings → null (recipe 테이블 미보유)</li>
 *   <li>author → null (공공레시피는 작성자 없음)</li>
 *   <li>tags → 빈 리스트 (태그 테이블 없음)</li>
 * </ul>
 * 명세 구조는 유지하되 값만 비워 프론트 호환성을 지킨다.</p>
 *
 * <p>nutrition은 식약처 영양정보(우리가 추가한 컬럼). 명세 원본엔 없지만
 * 확장 결정으로 추가된 필드로, 프론트에서 영양 표시에 활용한다.</p>
 */
public record RecipeDetail(
        Long recipeId,
        String title,
        String description,
        Integer cookTime,
        String difficulty,                  // null
        Integer servings,                   // null
        String thumbnailUrl,
        BigDecimal avgRating,
        long reviewCount,
        boolean isWishlisted,
        Object author,                      // null (공공레시피)
        List<String> tags,                  // 빈 리스트
        NutritionInfo nutrition,            // 식약처 영양정보 (확장)
        List<RecipeIngredientItem> ingredients,
        List<RecipeStepItem> steps
) {}