package com.fridgefamer.dto.response.ingredient;

/**
 * 식재료 자동완성 단건 응답 — API 명세 §12 F21.
 *
 * <pre>
 * { "ingredientDictId": 12, "name": "양파", "category": "채소" }
 * </pre>
 *
 * <p>냉장고 재료 입력 시 식재료 사전(ingredient_dictionary)에서
 * 부분일치로 추천되는 후보 1건. 보관기한/팁은 suggest 엔드포인트에서 별도 조회.</p>
 */
public record IngredientAutocompleteItem(
        Long ingredientDictId,
        String name,
        String category
) {}
