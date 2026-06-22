package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레시피 상세 응답 — API 명세 §4 F05.
 *
 * <p>스키마에 없는 difficulty/servings/author/tags는 제외(2026-06-04 결정).
 * 대신 식약처 영양정보(nutrition)를 포함한다(week-1에 의도적으로 적재된 데이터).
 * ingredients/steps는 각각 recipe_ingredient/recipe_step에서 조회.</p>
 */
public record RecipeDetail(
        Long recipeId,
        String title,
        String summary,
        String authorNote,
        String authorReview,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        int reviewCount,
        boolean isWishlisted,
        boolean isOwner,
        RecipeNutrition nutrition,
        List<RecipeIngredient> ingredients,
        List<RecipeStep> steps
) {}