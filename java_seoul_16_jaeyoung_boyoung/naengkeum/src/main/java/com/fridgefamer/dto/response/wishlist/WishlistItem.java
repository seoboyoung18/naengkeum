package com.fridgefamer.dto.response.wishlist;

import java.time.LocalDateTime;

/**
 * 찜 목록 단건 — 일반 레시피/AI 레시피 통합 표현 (API 명세 §6 F15).
 *
 * <p>type으로 구분: RECIPE면 recipeId·thumbnailUrl 채워지고 aiRecipeId=null,
 * AI면 aiRecipeId 채워지고 recipeId·thumbnailUrl=null. 프론트는 type으로 분기한다.</p>
 */
public record WishlistItem(
        Long wishlistId,
        String type,            // "RECIPE" | "AI"
        Long recipeId,
        Long aiRecipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        LocalDateTime createdAt
) {}
