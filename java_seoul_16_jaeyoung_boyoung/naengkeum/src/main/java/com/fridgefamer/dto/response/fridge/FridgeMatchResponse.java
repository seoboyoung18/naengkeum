package com.fridgefamer.dto.response.fridge;

import java.util.List;

/**
 * GET /api/fridge/match 200 응답 (F02) — 레시피 재료 중 보유 중인 재료의 id 목록.
 *
 * <pre>
 * { "ownedIngredientIds": [1, 3, 5] }
 * </pre>
 *
 * <p>recipe_ingredient.name 과 fridge_item.name 을 매칭하여, 보유한 레시피 재료의
 * ingredient_id(recipe_ingredient PK)를 반환한다. 프론트가 보유/미보유를 하이라이트.</p>
 */
public record FridgeMatchResponse(
        List<Long> ownedIngredientIds
) {}
