package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 자동완성 단건 — API 명세 §4 F05.
 *
 * <pre>{ "recipeId":1, "title":"계란볶음" }</pre>
 */
public record RecipeAutocompleteItem(
        Long recipeId,
        String title
) {}
