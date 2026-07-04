package com.fridgefamer.dto.response.wishlist;

/**
 * AI 레시피 찜 저장 응답 — API 명세 §6 F19. POST /api/wishlist/ai → 201.
 *
 * <pre>{ "aiRecipeId": 5 }</pre>
 */
public record AiRecipeSaved(
        Long aiRecipeId
) {}
