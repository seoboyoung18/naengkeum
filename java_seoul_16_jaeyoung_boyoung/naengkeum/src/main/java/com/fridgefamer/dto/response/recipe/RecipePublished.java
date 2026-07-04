package com.fridgefamer.dto.response.recipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * "공개하기" 응답 — PATCH /api/recipe/{recipeId}/publish.
 *
 * <p>공개 후 상태(is_public=true)를 돌려준다. 멱등 — 이미 공개여도 200/true.
 * 최초 공개 시 냉장고에서 차감된 재료 목록({@link #consumed()})을 함께 내려줘
 * 프론트가 "앞다리살 200g 차감됨" 토스트를 띄울 수 있게 한다. 차감이 없거나
 * 비공개 전환 응답이면 빈 리스트.</p>
 */
public record RecipePublished(Long recipeId, boolean isPublic, List<ConsumedItem> consumed) {

    /** 차감된 재료 1건 — name 만큼(used unit) 빠졌고, removed=true면 다 써서 냉장고에서 삭제됨. */
    public record ConsumedItem(String name, BigDecimal used, String unit, boolean removed) {}

    /** 차감 없는 응답(이미 공개됐던 경우·비공개 전환)용. */
    public static RecipePublished of(Long recipeId, boolean isPublic) {
        return new RecipePublished(recipeId, isPublic, List.of());
    }
}
