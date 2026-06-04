package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 자동완성 항목 — API 명세 §4 GET /api/recipe/autocomplete.
 *
 * <pre>[{ "recipeId": 1, "title": "계란볶음" }]</pre>
 */
public record RecipeAutocompleteItem(
        Long recipeId,
        String title
) {}