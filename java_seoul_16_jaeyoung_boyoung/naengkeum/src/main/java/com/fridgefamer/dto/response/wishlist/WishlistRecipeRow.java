package com.fridgefamer.dto.response.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 찜 목록(일반 레시피)의 DB 조회 1행 — Mapper 전용 중간 DTO.
 *
 * <p>mainIngredients(별도 묶음 조회 주입)를 제외한 행. 서비스에서 WishlistItem으로 조립.</p>
 */
public record WishlistRecipeRow(
        Long wishlistId,
        Long recipeId,
        String title,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        long reviewCount,
        LocalDateTime wishlistedAt
) {}