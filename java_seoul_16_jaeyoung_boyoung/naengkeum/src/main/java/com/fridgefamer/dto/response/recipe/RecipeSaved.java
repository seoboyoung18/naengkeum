package com.fridgefamer.dto.response.recipe;

/**
 * "내 레시피로 담기" 응답 — POST /api/recipe/from-ai/{aiRecipeId} (201).
 *
 * <p>생성된 recipe_id를 돌려준다. 프론트는 이 id로 상세 이동/공개하기를 이어간다.</p>
 */
public record RecipeSaved(Long recipeId) {}
