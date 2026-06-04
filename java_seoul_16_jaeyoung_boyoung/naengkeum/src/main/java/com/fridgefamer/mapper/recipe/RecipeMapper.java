package com.fridgefamer.mapper.recipe;

import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeCardRow;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredientItem;
import com.fridgefamer.dto.response.recipe.RecipeStepItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 레시피(recipe) 테이블 Mapper — Recipe 그룹 (API 명세 §4).
 *
 * <p>SQL은 src/main/resources/mapper/recipe/RecipeMapper.xml 에 정의.
 * 검색의 정렬(sort)/필터(keyword·ingredients·maxCookTime)는 문자열 연결 없이
 * XML의 &lt;choose&gt;/&lt;if&gt;로 분기한다(SQL injection 방어, Fridge 컨벤션 동일).</p>
 *
 * <p>avgRating/reviewCount는 review 테이블 LEFT JOIN 집계로 계산한다.</p>
 */
@Mapper
public interface RecipeMapper {

    // ---- 검색 ----

    /**
     * 레시피 검색 결과(페이지). 정렬·필터는 XML에서 안전하게 분기.
     * @param ingredients 재료명 리스트(없으면 null/빈 리스트)
     */
    List<RecipeCardRow> search(@Param("keyword") String keyword,
                               @Param("sort") String sort,
                               @Param("ingredients") List<String> ingredients,
                               @Param("maxCookTime") Integer maxCookTime,
                               @Param("offset") int offset,
                               @Param("size") int size);

    /** 검색 조건에 맞는 총 개수(페이징 totalElements). search와 동일 필터. */
    long countSearch(@Param("keyword") String keyword,
                     @Param("ingredients") List<String> ingredients,
                     @Param("maxCookTime") Integer maxCookTime);

    // ---- 자동완성 ----

    /** 레시피명 부분일치 자동완성(최대 limit개). */
    List<RecipeAutocompleteItem> autocomplete(@Param("keyword") String keyword,
                                              @Param("limit") int limit);

    // ---- 인기 ----

    /** 인기순(리뷰 수 → 평점 → 조회수) 상위 limit개. */
    List<RecipeCardRow> popular(@Param("limit") int limit);

    // ---- 상세 ----

    /** 레시피 본문 + 집계 + 영양. 없으면 null. */
    RecipeDetailRow selectDetail(@Param("recipeId") Long recipeId);

    /** 레시피 재료 목록(ingredient_id 순). */
    List<RecipeIngredientItem> selectIngredients(@Param("recipeId") Long recipeId);

    /** 레시피 조리 단계(step_number 순). */
    List<RecipeStepItem> selectSteps(@Param("recipeId") Long recipeId);

    /** 존재 여부(상세 404 판단 보조용, 필요 시). */
    boolean existsById(@Param("recipeId") Long recipeId);

    // ---- 보조: 카드 목록의 대표 재료 / 찜 여부 ----

    /** 여러 레시피의 대표 재료(각 레시피당 최대 3개)를 한 번에 조회. */
    List<RecipeMainIngredientRow> selectMainIngredients(@Param("recipeIds") List<Long> recipeIds);

    /** 회원이 찜한 recipe_id 목록(카드 isWishlisted 계산용). 비로그인 시 호출 안 함. */
    List<Long> selectWishlistedRecipeIds(@Param("memberId") Long memberId,
                                         @Param("recipeIds") List<Long> recipeIds);

    /** 단건 찜 여부(상세용). */
    boolean isWishlisted(@Param("memberId") Long memberId,
                         @Param("recipeId") Long recipeId);

    /** 카드 대표재료 조회 행(recipe_id + name). */
    record RecipeMainIngredientRow(Long recipeId, String name) {}
}