package com.fridgefamer.service;

import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.recipe.NutritionInfo;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeCard;
import com.fridgefamer.dto.response.recipe.RecipeCardRow;
import com.fridgefamer.dto.response.recipe.RecipeDetail;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredientItem;
import com.fridgefamer.dto.response.recipe.RecipeStepItem;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import com.fridgefamer.mapper.recipe.RecipeMapper.RecipeMainIngredientRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 레시피(Recipe) 도메인 서비스 — API 명세 §4.
 *
 * <p>레시피 검색/상세는 인증 불필요(공개). 다만 memberId가 있으면(로그인) 찜 여부를
 * 채워주고, 없으면(비로그인) isWishlisted=false로 둔다. Controller가 nullable memberId를
 * 넘긴다.</p>
 *
 * <p>스키마-명세 갭(1번 정책): difficulty/servings/author/tags는 데이터가 없어
 * null 또는 빈 값으로 응답. 구조는 유지한다.</p>
 */
@Service
public class RecipeService {

    /** 자동완성 최대 개수 (API 명세 §4). */
    private static final int AUTOCOMPLETE_LIMIT = 5;

    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    // =====================================================================
    //  GET /api/recipe — 검색
    // =====================================================================
    public PageResponse<RecipeCard> search(Long memberId,
                                           String keyword,
                                           String sort,
                                           List<String> ingredients,
                                           Integer maxCookTime,
                                           int page,
                                           int size) {
        int offset = page * size;
        List<RecipeCardRow> rows =
                recipeMapper.search(keyword, sort, ingredients, maxCookTime, offset, size);
        long total = recipeMapper.countSearch(keyword, ingredients, maxCookTime);

        List<RecipeCard> cards = toCards(memberId, rows);
        return PageResponse.of(cards, page, size, total);
    }

    // =====================================================================
    //  GET /api/recipe/autocomplete
    // =====================================================================
    public List<RecipeAutocompleteItem> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return recipeMapper.autocomplete(keyword.trim(), AUTOCOMPLETE_LIMIT);
    }

    // =====================================================================
    //  GET /api/recipe/popular
    // =====================================================================
    public List<RecipeCard> popular(Long memberId, int limit) {
        List<RecipeCardRow> rows = recipeMapper.popular(limit);
        return toCards(memberId, rows);
    }

    // =====================================================================
    //  GET /api/recipe/{recipeId} — 상세
    // =====================================================================
    public RecipeDetail detail(Long memberId, Long recipeId) {
        RecipeDetailRow row = recipeMapper.selectDetail(recipeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }

        List<RecipeIngredientItem> ingredients = recipeMapper.selectIngredients(recipeId);
        List<RecipeStepItem> steps = recipeMapper.selectSteps(recipeId);
        boolean wishlisted = memberId != null && recipeMapper.isWishlisted(memberId, recipeId);

        NutritionInfo nutrition = new NutritionInfo(
                row.calories(), row.carbs(), row.protein(), row.fat(), row.sodium());

        return new RecipeDetail(
                row.recipeId(),
                row.title(),
                row.description(),
                row.cookTime(),
                null,                 // difficulty — 스키마 미보유
                null,                 // servings   — 스키마 미보유
                row.thumbnailUrl(),
                row.avgRating(),
                row.reviewCount(),
                wishlisted,
                null,                 // author — 공공레시피는 작성자 없음
                List.of(),            // tags  — 태그 테이블 없음
                nutrition,
                ingredients,
                steps
        );
    }

    // =====================================================================
    //  내부 헬퍼 — 카드 행 → 카드 응답 조립 (대표재료 + 찜여부 주입)
    // =====================================================================
    private List<RecipeCard> toCards(Long memberId, List<RecipeCardRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> recipeIds = rows.stream().map(RecipeCardRow::recipeId).toList();

        // 1) 대표 재료(레시피당 최대 3개)를 한 번에 조회 → recipeId별 그룹핑
        Map<Long, List<String>> mainIngredientMap = new LinkedHashMap<>();
        for (RecipeMainIngredientRow mi : recipeMapper.selectMainIngredients(recipeIds)) {
            mainIngredientMap
                    .computeIfAbsent(mi.recipeId(), k -> new ArrayList<>())
                    .add(mi.name());
        }

        // 2) 로그인 시 찜한 recipe_id 집합 (비로그인은 빈 집합)
        Set<Long> wishlistedIds = (memberId == null)
                ? Collections.emptySet()
                : recipeMapper.selectWishlistedRecipeIds(memberId, recipeIds)
                        .stream().collect(Collectors.toSet());

        // 3) 조립
        List<RecipeCard> cards = new ArrayList<>(rows.size());
        for (RecipeCardRow row : rows) {
            cards.add(new RecipeCard(
                    row.recipeId(),
                    row.title(),
                    row.thumbnailUrl(),
                    row.cookTime(),
                    null,                                    // difficulty — null
                    row.avgRating(),
                    row.reviewCount(),
                    wishlistedIds.contains(row.recipeId()),
                    mainIngredientMap.getOrDefault(row.recipeId(), List.of())
            ));
        }
        return cards;
    }
}