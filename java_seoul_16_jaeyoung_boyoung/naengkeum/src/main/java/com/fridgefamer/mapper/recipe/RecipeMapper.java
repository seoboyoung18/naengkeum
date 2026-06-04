package com.fridgefamer.mapper.recipe;

import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeListRow;
import com.fridgefamer.dto.response.recipe.RecipeMainIngredient;
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

    /** 단일 레시피 전체 재료(등록 순). */
    List<RecipeIngredient> selectIngredients(@Param("recipeId") Long recipeId);

    /** 단일 레시피 조리 단계(step_number 순). */
    List<RecipeStep> selectSteps(@Param("recipeId") Long recipeId);

    /** 상세 조회 시 조회수 +1. */
    int increaseViewCount(@Param("recipeId") Long recipeId);
}
