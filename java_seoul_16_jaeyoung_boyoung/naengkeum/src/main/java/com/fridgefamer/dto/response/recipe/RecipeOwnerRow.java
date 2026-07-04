package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 소유/공개 상태 내부 행 — 공개하기 권한 검증용.
 *
 * <p>authorId가 NULL이면 공공(시드) 레시피 → 누구도 공개 토글 권한 없음.
 * 없으면(null row) 404. imageUrl은 "사진 없으면 공개 불가" 검증에 사용.
 * ingredientsConsumed는 공개 시 냉장고 재고 차감을 이미 수행했는지(재공개 중복 차감 방지).</p>
 */
public record RecipeOwnerRow(
        Long recipeId,
        Long authorId,
        Boolean isPublic,
        String imageUrl,
        boolean ingredientsConsumed
) {}
