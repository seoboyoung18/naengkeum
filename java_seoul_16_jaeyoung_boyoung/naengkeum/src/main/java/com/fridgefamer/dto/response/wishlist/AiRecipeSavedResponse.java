package com.fridgefamer.dto.response.wishlist;

/**
 * AI 레시피 저장 응답 — API 명세 §6 POST /api/wishlist/ai.
 *
 * <pre>{ "aiRecipeId": 5 }</pre>
 */
public record AiRecipeSavedResponse(
        Long aiRecipeId
) {}