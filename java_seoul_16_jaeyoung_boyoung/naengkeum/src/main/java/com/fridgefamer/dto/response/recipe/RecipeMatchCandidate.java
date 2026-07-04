package com.fridgefamer.dto.response.recipe;

/**
 * 냉장고 재료 ↔ DB 레시피 매칭 후보 — AI 추천의 DB 우선 단계에서 사용.
 *
 * <p>matched = 레시피 재료 중 사용자가 보유한 개수, total = 레시피 전체 재료 수,
 * buyNeeded = 보유하지도 기본양념도 아닌(= 실제로 사야 하는) 재료 수.
 * buyNeeded가 작을수록 "냉장고로 만들기 쉬운" 레시피. 조리단계가 있는 레시피만 후보로 둔다.</p>
 */
public record RecipeMatchCandidate(
        Long recipeId,
        String title,
        String summary,
        Integer cookTime,
        int total,
        int matched,
        int buyNeeded
) {}
