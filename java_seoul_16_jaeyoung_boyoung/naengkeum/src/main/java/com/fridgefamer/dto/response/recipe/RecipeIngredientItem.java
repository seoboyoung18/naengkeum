package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 상세의 재료 항목 — API 명세 §4 상세.
 *
 * <pre>{ "name": "계란", "qty": "2개", "sortOrder": 1 }</pre>
 *
 * <p>recipe_ingredient 테이블은 name(VARCHAR), qty(VARCHAR) 비정규화 보존 구조.
 * 명세의 qty/unit 분리는 스키마상 qty 한 컬럼에 합쳐져 있어 qty 문자열로 반환.
 * sortOrder는 컬럼이 없어 ingredient_id 순서를 부여(서비스에서 인덱싱).</p>
 */
public record RecipeIngredientItem(
        String name,
        String qty,
        int sortOrder
) {}