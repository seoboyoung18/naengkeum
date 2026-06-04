package com.fridgefamer.dto.response.recipe;

/**
 * 목록용 대표 재료 행 — (recipeId, name) 매핑.
 *
 * <p>여러 레시피의 상위 N개 재료를 한 쿼리로 가져와 서비스에서 recipeId별로 묶는다.</p>
 */
public record RecipeMainIngredient(
        Long recipeId,
        String name
) {}
