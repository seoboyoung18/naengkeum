package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 재료 단건 — recipe_ingredient.
 *
 * <p>qty는 정규화하지 않은 원본 표기 문자열("2개", "100g", "적당량").
 * 스키마에 unit/sortOrder 컬럼이 없어 응답에서 제외(2026-06-04 결정).</p>
 */
public record RecipeIngredient(
        String name,
        String qty
) {}
