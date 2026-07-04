package com.fridgefamer.mapper.recipe;

import com.fridgefamer.dto.response.recipe.MyRecipeItem;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetailRow;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeListRow;
import com.fridgefamer.dto.response.recipe.RecipeMainIngredient;
import com.fridgefamer.dto.response.recipe.RecipeMatchCandidate;
import com.fridgefamer.dto.response.recipe.RecipeOwnerRow;
import com.fridgefamer.dto.response.recipe.RecipeStep;
import com.fridgefamer.dto.response.wishlist.AiRecipeRow;
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
                                         @Param("minCookTime") Integer minCookTime,
                                         @Param("maxCookTime") Integer maxCookTime,
                                         @Param("sort") String sort,
                                         @Param("mine") boolean mine,
                                         @Param("viewerId") Long viewerId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    /** 검색 필터와 동일 조건의 전체 건수(페이징 totalElements용). mine=true면 author_id=viewerId 기준. */
    long countRecipes(@Param("keyword") String keyword,
                      @Param("ingredients") List<String> ingredients,
                      @Param("minCookTime") Integer minCookTime,
                      @Param("maxCookTime") Integer maxCookTime,
                      @Param("mine") boolean mine,
                      @Param("viewerId") Long viewerId);

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

    // =================================================================
    //  쓰기 — AI 레시피 "담기" / 공개 / 마이 레시피 (V6 author_id·is_public)
    // =================================================================

    /** "담기" 출처 ai_recipe 단건(JSON은 문자열로). 없으면 null. 소유자 검증은 서비스에서. */
    AiRecipeRow selectSourceAiRecipe(@Param("aiRecipeId") Long aiRecipeId);

    /**
     * ai_recipe를 recipe로 복사(담기). source='AI_SAVED', is_public=FALSE.
     * UNIQUE(author_id, source_ai_recipe_id) 위반 시 DuplicateKey → 409(이미 담음).
     */
    int insertRecipe(@Param("cmd") RecipeInsertCommand cmd);

    /** 담은 레시피의 재료 일괄 삽입. */
    int insertIngredients(@Param("recipeId") Long recipeId,
                          @Param("items") List<RecipeIngredient> items);

    /** 담은 레시피의 조리 단계 일괄 삽입. */
    int insertSteps(@Param("recipeId") Long recipeId,
                    @Param("steps") List<RecipeStep> steps);

    /** 공개하기 권한 검증용 소유/공개 상태. 없으면 null(404). */
    RecipeOwnerRow selectRecipeOwner(@Param("recipeId") Long recipeId);

    /** 공개로 전환(is_public=TRUE). 멱등. */
    int markPublic(@Param("recipeId") Long recipeId);

    /** 공개 시 냉장고 재고 차감을 1회만 수행하기 위한 멱등 플래그 셋(ingredients_consumed=TRUE). */
    int markIngredientsConsumed(@Param("recipeId") Long recipeId);

    /** 공개 → 비공개로 전환(is_public=FALSE). 멱등. */
    int markPrivate(@Param("recipeId") Long recipeId);

    /**
     * 레시피 삭제. recipe_ingredient/recipe_step/review/wishlist/report는
     * 모두 FK ON DELETE CASCADE라 함께 정리된다. 권한 검증은 서비스에서 선행.
     */
    int deleteRecipe(@Param("recipeId") Long recipeId);

    /** 레시피 대표 이미지 경로 갱신. 본인 레시피 검증은 서비스에서 선행. */
    int updateImageUrl(@Param("recipeId") Long recipeId,
                       @Param("imageUrl") String imageUrl);

    /** 작성자 후기 갱신. 본인 레시피 검증은 서비스에서 선행. null이면 후기 삭제. */
    int updateAuthorReview(@Param("recipeId") Long recipeId,
                           @Param("review") String review);

    /** 마이 레시피 목록(author_id=나, 공개/비공개 포함, 최신순). */
    List<MyRecipeItem> selectMyRecipes(@Param("memberId") Long memberId);

    /** 특정 작성자의 공개 레시피 목록(is_public=TRUE, 최신순). 타 유저 프로필용. */
    List<RecipeListRow> selectByAuthor(@Param("authorId") Long authorId,
                                       @Param("viewerId") Long viewerId);

    // =================================================================
    //  recipe insert 후 생성 키 회수용 (ai_recipe → recipe 복사)
    // =================================================================
    class RecipeInsertCommand {
        private Long recipeId;
        private final Long authorId;
        private final Long sourceAiRecipeId;
        private final String title;
        private final String summary;
        private final String authorNote;
        private final Integer cookTime;

        public RecipeInsertCommand(Long authorId, Long sourceAiRecipeId,
                                   String title, String summary, String authorNote,
                                   Integer cookTime) {
            this.authorId = authorId;
            this.sourceAiRecipeId = sourceAiRecipeId;
            this.title = title;
            this.summary = summary;
            this.authorNote = authorNote;
            this.cookTime = cookTime;
        }

        public Long getRecipeId()          { return recipeId; }
        public void setRecipeId(Long id)   { this.recipeId = id; }
        public Long getAuthorId()          { return authorId; }
        public Long getSourceAiRecipeId()  { return sourceAiRecipeId; }
        public String getTitle()           { return title; }
        public String getSummary()         { return summary; }
        public String getAuthorNote()      { return authorNote; }
        public Integer getCookTime()       { return cookTime; }
    }
}