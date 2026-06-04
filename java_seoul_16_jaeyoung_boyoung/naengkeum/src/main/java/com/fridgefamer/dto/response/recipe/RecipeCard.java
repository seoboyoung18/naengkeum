package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레시피 검색/인기 목록의 카드 단위 응답 — API 명세 §4.
 *
 * <pre>
 * {
 *   "recipeId": 1, "title": "계란볶음", "thumbnailUrl": "https://...",
 *   "cookTime": 20, "difficulty": null, "avgRating": 4.3, "reviewCount": 12,
 *   "isWishlisted": false, "mainIngredients": ["계란","당면","양파"]
 * }
 * </pre>
 *
 * <p>스키마-명세 갭(1번 정책): difficulty/servings는 recipe 테이블에 컬럼이 없어
 * 항상 null. 식약처 공공레시피엔 난이도 개념이 없으므로 솔직하게 null 반환한다.</p>
 *
 * <p>thumbnailUrl ← image_url, mainIngredients는 별도 조회 후 서비스에서 주입.
 * avgRating/reviewCount는 review 집계, isWishlisted는 로그인 시에만 의미(비로그인 false).</p>
 */
public record RecipeCard(
        Long recipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        String difficulty,          // 항상 null (스키마 미보유)
        BigDecimal avgRating,
        long reviewCount,
        boolean isWishlisted,
        List<String> mainIngredients
) {}