package com.fridgefamer.dto.response.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 찜 목록 항목 — API 명세 §6 GET /api/wishlist/me.
 *
 * <p>일반 레시피 찜을 카드 형태로 표현. RecipeCard와 유사하나 wishlist 메타
 * (wishlistId, wishlistedAt)를 포함한다. AI 레시피 찜은 별도 표기(type=AI).</p>
 *
 * <pre>
 * { "wishlistId":1, "type":"RECIPE", "recipeId":327, "title":"계란볶음",
 *   "thumbnailUrl":"...", "cookTime":20, "avgRating":4.3, "reviewCount":12,
 *   "mainIngredients":["계란","양파"], "wishlistedAt":"2026-06-04T..." }
 * </pre>
 */
public record WishlistItem(
        Long wishlistId,
        String type,                 // "RECIPE" | "AI"
        Long recipeId,               // 일반 레시피 찜일 때
        Long aiRecipeId,             // AI 레시피 찜일 때
        String title,
        String thumbnailUrl,
        Integer cookTime,
        BigDecimal avgRating,
        long reviewCount,
        List<String> mainIngredients,
        LocalDateTime wishlistedAt
) {}