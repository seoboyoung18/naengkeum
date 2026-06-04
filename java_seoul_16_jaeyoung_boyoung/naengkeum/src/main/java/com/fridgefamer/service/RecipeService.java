package com.fridgefamer.service;

import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetail;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeListItem;
import com.fridgefamer.dto.response.recipe.RecipeListRow;
import com.fridgefamer.dto.response.recipe.RecipeMainIngredient;
import com.fridgefamer.dto.response.recipe.RecipeNutrition;
import com.fridgefamer.dto.response.recipe.RecipeStep;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 레시피(Recipe) 도메인 서비스 — API 명세 §4.
 *
 * <p>모두 인증 선택(viewerId nullable). viewerId가 있으면 isWishlisted가 채워지고,
 * 없으면 false. 목록은 본문+집계를 한 방에 조회한 뒤 대표 재료만 일괄 조회로 enrich(N+1 회피).</p>
 */
@Service
public class RecipeService {

    /** 자동완성 최대 노출 건수(API 명세 §4). */
    private static final int AUTOCOMPLETE_LIMIT = 5;

    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    // =====================================================================
    //  GET /api/recipe — 검색/필터/정렬 + 페이징
    // =====================================================================
    public PageResponse<RecipeListItem> search(Long viewerId, String keyword, List<String> ingredients,
                                               Integer maxCookTime, String sort, int page, int size) {
        int offset = page * size;
        List<RecipeListRow> rows =
                recipeMapper.selectRecipePage(keyword, ingredients, maxCookTime, sort, viewerId, offset, size);
        long total = recipeMapper.countRecipes(keyword, ingredients, maxCookTime);
        return PageResponse.of(enrich(rows), page, size, total);
    }

    // =====================================================================
    //  GET /api/recipe/popular — 인기순 Top N
    // =====================================================================
    public List<RecipeListItem> popular(Long viewerId, int limit) {
        return enrich(recipeMapper.selectPopular(viewerId, limit));
    }

    // =====================================================================
    //  GET /api/recipe/autocomplete — 제목 자동완성
    // =====================================================================
    public List<RecipeAutocompleteItem> autocomplete(String keyword) {
        return recipeMapper.selectAutocomplete(keyword.trim(), AUTOCOMPLETE_LIMIT);
    }

    // =====================================================================
    //  GET /api/recipe/{recipeId} — 상세 (+조회수 증가)
    // =====================================================================
    @Transactional
    public RecipeDetail detail(Long viewerId, Long recipeId) {
        RecipeDetailRow row = recipeMapper.selectDetail(recipeId, viewerId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        recipeMapper.increaseViewCount(recipeId);

        List<RecipeIngredient> ingredients = recipeMapper.selectIngredients(recipeId);
        List<RecipeStep> steps = recipeMapper.selectSteps(recipeId);
        RecipeNutrition nutrition = new RecipeNutrition(
                row.calories(), row.carbs(), row.protein(), row.fat(), row.sodium());

        return new RecipeDetail(
                row.recipeId(), row.title(), row.summary(), row.thumbnailUrl(), row.cookTime(),
                row.avgRating(), row.reviewCount(), row.isWishlisted(),
                nutrition, ingredients, steps);
    }

    // =====================================================================
    //  내부 헬퍼 — 목록 row에 대표 재료(상위 3) 붙이기 (N+1 회피)
    // =====================================================================
    private List<RecipeListItem> enrich(List<RecipeListRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(RecipeListRow::recipeId).toList();

        Map<Long, List<String>> ingredientsByRecipe = new LinkedHashMap<>();
        for (RecipeMainIngredient mi : recipeMapper.selectMainIngredients(ids)) {
            ingredientsByRecipe.computeIfAbsent(mi.recipeId(), k -> new ArrayList<>()).add(mi.name());
        }

        return rows.stream()
                .map(r -> new RecipeListItem(
                        r.recipeId(), r.title(), r.thumbnailUrl(), r.cookTime(),
                        r.avgRating(), r.reviewCount(), r.isWishlisted(),
                        ingredientsByRecipe.getOrDefault(r.recipeId(), List.of())))
                .toList();
    }
}
