package com.fridgefamer.service;

import com.fridgefamer.dto.response.ingredient.IngredientAutocompleteItem;
import com.fridgefamer.dto.response.ingredient.IngredientSuggestResponse;
import com.fridgefamer.mapper.ingredient.IngredientMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 식재료 사전(Ingredient) 도메인 서비스 — API 명세 §12.
 *
 * <p>읽기 전용. 인증 불필요 엔드포인트라 memberId를 받지 않는다.
 * suggest는 사전에 없으면 404 대신 직접입력 폴백 응답을 만들어 커버리지를 보장한다.</p>
 */
@Service
public class IngredientService {

    /** 자동완성 최대 노출 건수. */
    private static final int AUTOCOMPLETE_LIMIT = 10;

    /** 폴백(직접입력) 시 UI 기본 보관 위치. */
    private static final String FALLBACK_STORAGE_TYPE = "FRIDGE";
    private static final String FALLBACK_TIP =
            "사전에 등록되지 않은 식재료입니다. 보관 위치와 기한을 직접 입력해 주세요.";

    private final IngredientMapper ingredientMapper;

    public IngredientService(IngredientMapper ingredientMapper) {
        this.ingredientMapper = ingredientMapper;
    }

    // =====================================================================
    //  GET /api/ingredients/autocomplete
    // =====================================================================
    public List<IngredientAutocompleteItem> autocomplete(String keyword) {
        return ingredientMapper.selectAutocomplete(keyword.trim(), AUTOCOMPLETE_LIMIT);
    }

    // =====================================================================
    //  GET /api/ingredients/suggest
    // =====================================================================
    public IngredientSuggestResponse suggest(String name) {
        IngredientSuggestResponse found = ingredientMapper.selectByName(name.trim());
        return found != null ? found : fallback(name.trim());
    }

    /** 사전 미등록 식재료의 직접입력 폴백 응답. */
    private IngredientSuggestResponse fallback(String name) {
        return new IngredientSuggestResponse(
                name,
                null,                    // category 미상
                FALLBACK_STORAGE_TYPE,   // 기본 보관 위치(편집 가능)
                null, null, null,        // 보관 일수 미상
                FALLBACK_TIP,
                false                    // found=false → 프론트 직접입력 모드
        );
    }
}
