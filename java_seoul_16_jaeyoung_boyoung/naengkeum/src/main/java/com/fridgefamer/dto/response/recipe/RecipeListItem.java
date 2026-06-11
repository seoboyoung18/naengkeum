package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레시피 목록/인기 단건 응답 — API 명세 §4 F05.
 *
 * <pre>
 * { "recipeId":1, "title":"계란볶음", "thumbnailUrl":"https://...",
 *   "cookTime":20, "avgRating":4.3, "reviewCount":12,
 *   "isWishlisted":false, "mainIngredients":["계란","당면","양파"] }
 * </pre>
 *
 * <p>스키마에 없는 difficulty는 제외(2026-06-04 결정).
 * avgRating/reviewCount는 review 집계, isWishlisted는 wishlist에서 파생,
 * mainIngredients는 recipe_ingredient 상위 3건.</p>
 */
public record RecipeListItem(
        Long recipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        int reviewCount,
        boolean isWishlisted,
        String source,
        List<String> mainIngredients
) {}
