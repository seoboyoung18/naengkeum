package com.fridgefamer.mapper.wishlist;

import com.fridgefamer.dto.response.wishlist.WishlistRecipeRow;
import com.fridgefamer.mapper.recipe.RecipeMapper.RecipeMainIngredientRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 찜(wishlist) + AI 레시피(ai_recipe) Mapper — Wishlist 그룹 (API 명세 §6).
 *
 * <p>SQL은 src/main/resources/mapper/wishlist/WishlistMapper.xml 에 정의.
 * wishlist 테이블은 chk_wishlist_xor 제약으로 recipe_id/ai_recipe_id 중 정확히 하나만
 * 채워야 한다. 일반 찜은 recipe_id만, AI 찜은 ai_recipe_id만 INSERT한다.</p>
 */
@Mapper
public interface WishlistMapper {

    // ---- 일반 레시피 찜 ----

    /** 레시피 존재 여부(찜 대상 검증). */
    boolean recipeExists(@Param("recipeId") Long recipeId);

    /** 이미 찜했는지(중복 409 판단). */
    boolean existsRecipeWish(@Param("memberId") Long memberId,
                             @Param("recipeId") Long recipeId);

    /** 일반 레시피 찜 등록. */
    int insertRecipeWish(@Param("memberId") Long memberId,
                         @Param("recipeId") Long recipeId);

    /** 일반 레시피 찜 해제. 삭제된 행 수 반환(0이면 찜 없었음). */
    int deleteRecipeWish(@Param("memberId") Long memberId,
                         @Param("recipeId") Long recipeId);

    // ---- 찜 목록 (일반 레시피) ----

    /** 회원의 일반 레시피 찜 목록(최근 찜 순, 페이지). */
    List<WishlistRecipeRow> selectRecipeWishes(@Param("memberId") Long memberId,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /** 회원의 일반 레시피 찜 총 개수. */
    long countRecipeWishes(@Param("memberId") Long memberId);

    /** 카드 대표재료(레시피당 3개) 묶음 조회 — RecipeMapper와 동일 구조 재사용. */
    List<RecipeMainIngredientRow> selectMainIngredients(@Param("recipeIds") List<Long> recipeIds);

    // ---- AI 레시피 저장/삭제 ----

    /**
     * AI 레시피 INSERT. ingredients_json/steps_json은 JSON 문자열로 전달
     * (서비스에서 List → JSON 직렬화). useGeneratedKeys로 ai_recipe_id 회수.
     */
    int insertAiRecipe(@Param("cmd") AiRecipeCommand cmd);

    /** AI 레시피를 찜에 등록(ai_recipe_id만 채움 — XOR). */
    int insertAiWish(@Param("memberId") Long memberId,
                     @Param("aiRecipeId") Long aiRecipeId);

    /** AI 레시피 소유자 회원 id 조회(본인 검증용). 없으면 null. */
    Long findAiRecipeOwner(@Param("aiRecipeId") Long aiRecipeId);

    /** AI 찜 + AI 레시피 삭제(찜 해제). ai_recipe 삭제 시 wishlist는 CASCADE. */
    int deleteAiRecipe(@Param("aiRecipeId") Long aiRecipeId);

    // =================================================================
    //  insert용 커맨드 (useGeneratedKeys 회수, Fridge/Review 컨벤션 동일)
    // =================================================================
    class AiRecipeCommand {
        private Long aiRecipeId;
        private final Long memberId;
        private final String title;
        private final String summary;
        private final String ingredientsJson;   // JSON 직렬화 문자열
        private final String stepsJson;          // JSON 직렬화 문자열
        private final Integer cookTime;

        public AiRecipeCommand(Long memberId, String title, String summary,
                               String ingredientsJson, String stepsJson, Integer cookTime) {
            this.memberId = memberId;
            this.title = title;
            this.summary = summary;
            this.ingredientsJson = ingredientsJson;
            this.stepsJson = stepsJson;
            this.cookTime = cookTime;
        }

        public Long getAiRecipeId()        { return aiRecipeId; }
        public void setAiRecipeId(Long id) { this.aiRecipeId = id; }
        public Long getMemberId()          { return memberId; }
        public String getTitle()           { return title; }
        public String getSummary()         { return summary; }
        public String getIngredientsJson() { return ingredientsJson; }
        public String getStepsJson()       { return stepsJson; }
        public Integer getCookTime()       { return cookTime; }
    }
}