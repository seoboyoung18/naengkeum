package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 상세의 조리 단계 — API 명세 §4 상세.
 *
 * <pre>{ "stepNumber": 1, "description": "팬에 기름을 두릅니다.", "imageUrl": null }</pre>
 */
public record RecipeStepItem(
        int stepNumber,
        String description,
        String imageUrl
) {}