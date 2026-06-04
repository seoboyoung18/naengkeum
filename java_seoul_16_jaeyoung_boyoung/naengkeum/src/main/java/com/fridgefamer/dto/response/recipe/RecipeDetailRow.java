package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;

/**
 * 레시피 상세의 recipe 본문 DB 조회 1행 — Mapper 전용 중간 DTO.
 *
 * <p>재료/스텝(별도 조회)과 isWishlisted(회원별 계산)를 제외한 본문 + 집계 + 영양.
 * 서비스에서 재료/스텝/찜여부를 합쳐 RecipeDetail로 조립한다.</p>
 */
public record RecipeDetailRow(
        Long recipeId,
        String title,
        String description,     // summary 컬럼 alias
        Integer cookTime,
        String thumbnailUrl,    // image_url alias
        BigDecimal avgRating,
        long reviewCount,
        // 영양정보 (식약처 제공)
        Integer calories,
        BigDecimal carbs,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal sodium
) {}