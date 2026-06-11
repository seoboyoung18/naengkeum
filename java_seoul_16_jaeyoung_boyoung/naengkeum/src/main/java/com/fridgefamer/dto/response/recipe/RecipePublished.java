package com.fridgefamer.dto.response.recipe;

/**
 * "공개하기" 응답 — PATCH /api/recipe/{recipeId}/publish.
 *
 * <p>공개 후 상태(is_public=true)를 돌려준다. 멱등 — 이미 공개여도 200/true.</p>
 */
public record RecipePublished(Long recipeId, boolean isPublic) {}
