package com.fridgefamer.mapper.recipe;

import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeListRow;
import com.fridgefamer.dto.response.recipe.RecipeMainIngredient;
import com.fridgefamer.dto.response.recipe.RecipeMatchCandidate;
import com.fridgefamer.dto.response.recipe.RecipeStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 레시피(recipe/recipe_ingredient/recipe_step) Mapper — Recipe 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/recipe/RecipeMapper.xml.
 * 목록/상세 모두 viewerId(인증 선택)를 받아 isWishlisted를 EXISTS로 계산하며,
 * 미인증(viewerId=null)이면 항상 false. 검색 필터는 &lt;sql&gt; 조각으로 select/count가 공유한다.</p>
 */
@Mapper
public interface RecipeMapper {

    /**
     * 검색/필터/정렬 + 페이징된 레시피 목록.
     * @param keyword     제목 또는 재료명 부분일치 (null/빈값이면 미적용)
     * @param ingredients 재료명 목록 — 하나라도 포함하는 레시피 (null/빈리스트면 미적용)
     * @param maxCookTime 최대 조리시간(분) (null이면 미적용)
     * @param sort        LATEST|POPULAR|RATING|COOK_TIME
     */
    List<RecipeListRow> selectRecipePage(@Param("keyword") String keyword,
                                         @Param("ingredients") List<String> ingredients,
                                         @Param("maxCookTime") Integer maxCookTime,
                                         @Param("sort") String sort,
                                         @Param("viewerId") Long viewerId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    /** 검색 필터와 동일 조건의 전체 건수(페이징 totalElements용). */
    long countRecipes(@Param("keyword") String keyword,
                      @Param("ingredients") List<String> ingredients,
                      @Param("maxCookTime") Integer maxCookTime);

    /** 인기순(view_count) Top N. */
    List<RecipeListRow> selectPopular(@Param("viewerId") Long viewerId,
                                      @Param("limit") int limit);

    /** 제목 부분일치 자동완성(접두 우선). */
    List<RecipeAutocompleteItem> selectAutocomplete(@Param("keyword") String keyword,
                                                    @Param("limit") int limit);

    /** 상세 본문 + 영양정보 + 집계. 없으면 null. */
    RecipeDetailRow selectDetail(@Param("recipeId") Long recipeId,
                                 @Param("viewerId") Long viewerId);

    /** 여러 레시피의 상위 3개 대표 재료명 (목록 enrich용). */
    List<RecipeMainIngredient> selectMainIngredients(@Param("recipeIds") List<Long> recipeIds);

    /**
     * "냉장고로 현실적으로 만들 수 있는" DB 레시피 후보 (AI 추천의 DB 우선 단계).
     * 보유 재료가 minMatch개 이상 겹치고, 사야 하는 재료(보유분·기본양념 제외)가 maxBuy개 이하인
     * 레시피만, 사야 할 게 적은 순으로 반환한다. 조리단계가 있는 레시피만 후보.
     * @param names    냉장고 보유 재료명(비어있지 않음)
     * @param staples  기본양념 등 "사야 할 것"에서 제외할 재료명(비어있지 않음)
     * @param minMatch 보유 재료 최소 겹침 수
     * @param maxBuy   사야 하는 재료 허용 최대 수
     * @param limit    후보 최대 개수
     */
    List<RecipeMatchCandidate> selectFridgeMatches(@Param("names") List<String> names,
                                                   @Param("staples") List<String> staples,
                                                   @Param("minMatch") int minMatch,
                                                   @Param("maxBuy") int maxBuy,
                                                   @Param("limit") int limit);

    /** 단일 레시피 전체 재료(등록 순). */
    List<RecipeIngredient> selectIngredients(@Param("recipeId") Long recipeId);

    /** 단일 레시피 조리 단계(step_number 순). */
    List<RecipeStep> selectSteps(@Param("recipeId") Long recipeId);

    /** 상세 조회 시 조회수 +1. */
    int increaseViewCount(@Param("recipeId") Long recipeId);
}
