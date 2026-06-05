package com.fridgefamer.mapper.wishlist;

import com.fridgefamer.dto.response.wishlist.AiRecipeRow;
import com.fridgefamer.dto.response.wishlist.WishlistItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 찜(wishlist) + AI 레시피(ai_recipe) Mapper — Wishlist 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/wishlist/WishlistMapper.xml.
 * 목록은 일반/AI 레시피를 UNION ALL로 합쳐 최신순. 중복 찜은 UNIQUE → 409 자동 변환.
 * AI 찜 해제는 ai_recipe 삭제 → FK CASCADE로 wishlist 행도 함께 제거된다.</p>
 */
@Mapper
public interface WishlistMapper {

    // ---- 목록 ----

    /** 회원 찜 목록(일반+AI, 최신순, 페이징). */
    List<WishlistItem> selectByMember(@Param("memberId") Long memberId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countByMember(@Param("memberId") Long memberId);

    // ---- 일반 레시피 찜 ----

    /** 일반 레시피 찜 등록. 중복 시 UNIQUE 위반(→409), 없는 레시피면 FK 위반(→400). */
    int insertRecipeWishlist(@Param("memberId") Long memberId,
                             @Param("recipeId") Long recipeId);

    /** 일반 레시피 찜 해제. 멱등(없어도 0행). */
    int deleteRecipeWishlist(@Param("memberId") Long memberId,
                             @Param("recipeId") Long recipeId);

    // ---- AI 레시피 찜 ----

    /** ai_recipe 저장(생성 키 회수). */
    int insertAiRecipe(@Param("cmd") AiRecipeCommand cmd);

    /** 저장된 ai_recipe를 wishlist에 연결. */
    int insertAiWishlist(@Param("memberId") Long memberId,
                         @Param("aiRecipeId") Long aiRecipeId);

    /** ai_recipe 소유자 member_id — 권한 검증용. 없으면 null. */
    Long findAiRecipeOwner(@Param("aiRecipeId") Long aiRecipeId);

    /** ai_recipe 단건(JSON은 문자열로). 없으면 null. 소유자 검증은 서비스에서. */
    AiRecipeRow selectAiRecipe(@Param("aiRecipeId") Long aiRecipeId);

    /** ai_recipe 삭제(FK CASCADE로 wishlist 행 동반 삭제). */
    int deleteAiRecipe(@Param("aiRecipeId") Long aiRecipeId);

    // =================================================================
    //  ai_recipe insert 후 키 회수용
    // =================================================================
    class AiRecipeCommand {
        private Long aiRecipeId;
        private final Long memberId;
        private final String title;
        private final String summary;
        private final String ingredientsJson;   // 직렬화된 JSON 배열 문자열
        private final String stepsJson;          // 직렬화된 JSON 배열 문자열
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
