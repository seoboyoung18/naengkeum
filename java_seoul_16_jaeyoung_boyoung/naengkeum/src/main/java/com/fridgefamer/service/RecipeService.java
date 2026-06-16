package com.fridgefamer.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.recipe.MyRecipeItem;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetail;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeListItem;
import com.fridgefamer.dto.response.recipe.RecipeListRow;
import com.fridgefamer.dto.response.recipe.RecipeMainIngredient;
import com.fridgefamer.dto.response.recipe.RecipeNutrition;
import com.fridgefamer.dto.response.recipe.RecipeOwnerRow;
import com.fridgefamer.dto.response.recipe.RecipePublished;
import com.fridgefamer.dto.response.recipe.RecipeSaved;
import com.fridgefamer.dto.response.recipe.RecipeStep;
import com.fridgefamer.dto.response.wishlist.AiRecipeRow;
import com.fridgefamer.config.SecurityUtils;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import com.fridgefamer.mapper.recipe.RecipeMapper.RecipeInsertCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** ai_recipe의 JSON 배열 파싱용(프로젝트 방침: ObjectMapper 빈 비의존, 자체 인스턴스). */
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP = new TypeReference<>() {};

    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    // =====================================================================
    //  GET /api/recipe — 검색/필터/정렬 + 페이징
    // =====================================================================
    public PageResponse<RecipeListItem> search(Long viewerId, String keyword, List<String> ingredients,
                                               Integer minCookTime, Integer maxCookTime, String sort,
                                               boolean mine, int page, int size) {
        // "내가 등록한"은 인증 필요 — 미인증이면 빈 결과(공공 레시피 author_id=null 노출 방지)
        if (mine && viewerId == null) {
            return PageResponse.of(List.of(), page, size, 0);
        }
        int offset = page * size;
        List<RecipeListRow> rows = recipeMapper.selectRecipePage(
                keyword, ingredients, minCookTime, maxCookTime, sort, mine, viewerId, offset, size);
        long total = recipeMapper.countRecipes(keyword, ingredients, minCookTime, maxCookTime, mine, viewerId);
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

        // 본인이 등록한 레시피인지 — 사진 업로드 버튼 노출 판단용 (비로그인/공공 레시피는 false)
        boolean isOwner = viewerId != null
                && row.authorId() != null
                && row.authorId().equals(viewerId);

        return new RecipeDetail(
                row.recipeId(), row.title(), row.summary(), row.thumbnailUrl(), row.cookTime(),
                row.avgRating(), row.reviewCount(), row.isWishlisted(), isOwner,
                nutrition, ingredients, steps);
    }

    // =====================================================================
    //  POST /api/recipe/from-ai/{aiRecipeId} — "내 레시피로 담기"
    //  ai_recipe를 recipe로 복사(스냅샷). 개인 보관(is_public=false), 내 소유.
    // =====================================================================
    @Transactional
    public RecipeSaved createFromAi(Long memberId, Long aiRecipeId) {
        AiRecipeRow ai = recipeMapper.selectSourceAiRecipe(aiRecipeId);
        if (ai == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "AI 레시피를 찾을 수 없습니다");
        }
        if (!ai.memberId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 AI 레시피만 담을 수 있습니다");
        }

        // 중복 담기는 UNIQUE(author_id, source_ai_recipe_id) → DuplicateKey → 409 (자동 변환)
        RecipeInsertCommand cmd = new RecipeInsertCommand(
                memberId, aiRecipeId, ai.title(), ai.summary(), ai.cookTime());
        recipeMapper.insertRecipe(cmd);
        Long recipeId = cmd.getRecipeId();

        List<RecipeIngredient> ingredients = parseIngredients(ai.ingredientsJson());
        if (!ingredients.isEmpty()) {
            recipeMapper.insertIngredients(recipeId, ingredients);
        }
        List<RecipeStep> steps = parseSteps(ai.stepsJson());
        if (!steps.isEmpty()) {
            recipeMapper.insertSteps(recipeId, steps);
        }
        return new RecipeSaved(recipeId);
    }

    // =====================================================================
    //  POST /api/recipe/from-ai — AI 결과 콘텐츠를 바로 "담기"
    //  (찜과 무관. D4 화면에서 갓 생성된 결과는 aiRecipeId가 없으므로 콘텐츠로 직접 등록)
    // =====================================================================
    @Transactional
    public RecipeSaved createFromAiContent(Long memberId, SaveAiRecipeRequest req) {
        // 콘텐츠는 ai_recipe를 거치지 않으므로 source_ai_recipe_id=null (중복 방지 미적용)
        RecipeInsertCommand cmd = new RecipeInsertCommand(
                memberId, null, req.title(), req.summary(), req.cookTime());
        recipeMapper.insertRecipe(cmd);
        Long recipeId = cmd.getRecipeId();

        List<RecipeIngredient> ingredients = parseIngredients(toJsonArray(req.ingredientsJson()));
        if (!ingredients.isEmpty()) {
            recipeMapper.insertIngredients(recipeId, ingredients);
        }
        List<RecipeStep> steps = parseSteps(toJsonArray(req.stepsJson()));
        if (!steps.isEmpty()) {
            recipeMapper.insertSteps(recipeId, steps);
        }
        return new RecipeSaved(recipeId);
    }

    /** List&lt;JsonNode&gt; → JSON 배열 문자열(파싱 재사용용). WishlistService와 동일 방침(Jackson 빈 비의존). */
    private String toJsonArray(List<JsonNode> nodes) {
        return nodes.stream()
                .map(JsonNode::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    // =====================================================================
    //  PATCH /api/recipe/{recipeId}/publish — "공개하기"
    // =====================================================================
    @Transactional
    public RecipePublished publish(Long memberId, Long recipeId) {
        RecipeOwnerRow row = recipeMapper.selectRecipeOwner(recipeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        if (row.authorId() == null || !row.authorId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인이 등록한 레시피만 공개할 수 있습니다");
        }
        if (!Boolean.TRUE.equals(row.isPublic())) {
            recipeMapper.markPublic(recipeId);
        }
        return new RecipePublished(recipeId, true);
    }

    // =====================================================================
    //  GET /api/recipe/mine — 마이 레시피 (author_id=나, 공개/비공개 포함)
    // =====================================================================
    public List<MyRecipeItem> listMine(Long memberId) {
        return recipeMapper.selectMyRecipes(memberId);
    }

    // =====================================================================
    //  DELETE /api/recipe/{recipeId} — 본인 또는 관리자 삭제
    //  자식(재료/단계/리뷰/찜/신고)은 FK ON DELETE CASCADE로 함께 삭제.
    // =====================================================================
    @Transactional
    public void delete(Long memberId, Long recipeId) {
        RecipeOwnerRow row = recipeMapper.selectRecipeOwner(recipeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        boolean owner = row.authorId() != null && row.authorId().equals(memberId);
        if (!owner && !SecurityUtils.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 또는 관리자만 삭제할 수 있습니다");
        }
        recipeMapper.deleteRecipe(recipeId);
    }

    // =====================================================================
    //  특정 작성자의 공개 레시피 목록 (타 유저 프로필 D11용, is_public=TRUE만)
    // =====================================================================
    public List<RecipeListItem> listPublicByAuthor(Long authorId, Long viewerId) {
        return enrich(recipeMapper.selectByAuthor(authorId, viewerId));
    }

    // =====================================================================
    //  내부 헬퍼 — ai_recipe JSON 배열 → recipe_ingredient/step 행
    // =====================================================================

    /** [{name, qty, unit}] → RecipeIngredient(name, "qty+unit"). 파싱 실패/빈값은 건너뜀. */
    private List<RecipeIngredient> parseIngredients(String json) {
        List<RecipeIngredient> out = new ArrayList<>();
        for (Map<String, Object> m : readArray(json)) {
            String name = str(m.get("name"));
            if (name == null || name.isBlank()) continue;
            out.add(new RecipeIngredient(name, joinQty(m.get("qty"), m.get("unit"))));
        }
        return out;
    }

    /** [{stepNumber, description}] → RecipeStep(번호, 설명). 번호 없으면 1부터 순번. */
    private List<RecipeStep> parseSteps(String json) {
        List<RecipeStep> out = new ArrayList<>();
        int seq = 1;
        for (Map<String, Object> m : readArray(json)) {
            String desc = str(m.get("description"));
            if (desc == null || desc.isBlank()) continue;
            Object sn = m.get("stepNumber");
            int number = (sn instanceof Number n) ? n.intValue() : seq;
            out.add(new RecipeStep(number, desc, null));
            seq++;
        }
        return out;
    }

    /** JSON 배열 문자열 → List&lt;Map&gt;. 파싱 실패 시 빈 리스트(레시피 본문은 유지). */
    private List<Map<String, Object>> readArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> v = JSON.readValue(json, LIST_OF_MAP);
            return v != null ? v : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** qty(숫자/문자) + unit → "2개", "0.5단" 같은 표기 문자열. 둘 다 비면 null. */
    private String joinQty(Object qty, Object unit) {
        String q = str(qty);
        String u = str(unit);
        String s = ((q == null ? "" : q) + (u == null ? "" : u)).trim();
        return s.isEmpty() ? null : s;
    }

    /** Object → String. 정수형 double("2.0")은 "2"로 정리. null은 null. */
    private String str(Object o) {
        if (o == null) return null;
        if (o instanceof Double d && d == Math.floor(d) && !d.isInfinite()) {
            return Long.toString(d.longValue());
        }
        return String.valueOf(o);
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
                        r.avgRating(), r.reviewCount(), r.isWishlisted(), r.source(),
                        ingredientsByRecipe.getOrDefault(r.recipeId(), List.of())))
                .toList();
    }
}