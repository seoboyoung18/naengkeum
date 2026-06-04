package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 카드 목록의 DB 조회 1행 — Mapper 전용 중간 DTO.
 *
 * <p>RecipeCard에서 mainIngredients(별도 조회 주입)와 isWishlisted(회원별 계산)를
 * 제외한 순수 집계 행. 서비스에서 RecipeCard로 조립한다.</p>
 */
public record RecipeCardRow(
        Long recipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        long reviewCount
) {}