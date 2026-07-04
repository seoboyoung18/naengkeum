package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 조리 단계 단건 — recipe_step.
 *
 * <pre>{ "stepNumber":1, "description":"팬에 기름을 두릅니다.", "imageUrl":null }</pre>
 */
public record RecipeStep(
        int stepNumber,
        String description,
        String imageUrl
) {}
