package com.fridgefamer.mapper.review;

import com.fridgefamer.dto.response.review.ReviewItem;
import com.fridgefamer.dto.response.review.ReviewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 리뷰(review) 테이블 Mapper — Review 그룹 (API 명세 §5).
 *
 * <p>SQL은 src/main/resources/mapper/review/ReviewMapper.xml 에 정의.
 * 작성 시 중복은 DB UNIQUE(uq_review_member_recipe)로도 막히지만, 서비스에서
 * 사전 체크 후 409(CONFLICT)를 명확히 던진다. 수정/삭제는 소유자 검증(findAuthorId).</p>
 */
@Mapper
public interface ReviewMapper {

    // ---- 조회 ----

    /** 레시피의 리뷰 목록(최신순, 페이지). */
    List<ReviewRow> selectByRecipe(@Param("recipeId") Long recipeId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    /** 레시피 리뷰 총 개수(페이징 totalElements). */
    long countByRecipe(@Param("recipeId") Long recipeId);

    /** 평점별 개수(통계 dist용). 평점 1~5 중 존재하는 것만 행으로. */
    List<RatingCountRow> selectRatingDistribution(@Param("recipeId") Long recipeId);

    /** 단건 조회(작성/수정 응답 생성용). 없으면 null. */
    ReviewItem selectItemById(@Param("reviewId") Long reviewId,
                              @Param("viewerId") Long viewerId);

    /** 작성자 회원 id만 조회 — 권한 검증용. 없으면 null. */
    Long findAuthorId(@Param("reviewId") Long reviewId);

    /** 회원이 이미 해당 레시피에 리뷰했는지(중복 작성 사전 체크). */
    boolean existsByMemberAndRecipe(@Param("memberId") Long memberId,
                                    @Param("recipeId") Long recipeId);

    /** 레시피 존재 여부(리뷰 작성 대상 검증용 — 없는 레시피에 리뷰 방지). */
    boolean recipeExists(@Param("recipeId") Long recipeId);

    // ---- 변경 ----

    int insertReview(@Param("cmd") ReviewCommand cmd);

    int updateReview(@Param("reviewId") Long reviewId,
                     @Param("rating") int rating,
                     @Param("content") String content);

    int deleteById(@Param("reviewId") Long reviewId);

    // =================================================================
    //  내부 인자/조회 클래스
    // =================================================================

    /** 평점-개수 1행 (dist 조립용). */
    record RatingCountRow(int rating, long count) {}

    /** insert용 — useGeneratedKeys 회수. record 불변이라 클래스로(Fridge 컨벤션 동일). */
    class ReviewCommand {
        private Long reviewId;
        private final Long memberId;
        private final Long recipeId;
        private final int rating;
        private final String content;

        public ReviewCommand(Long memberId, Long recipeId, int rating, String content) {
            this.memberId = memberId;
            this.recipeId = recipeId;
            this.rating = rating;
            this.content = content;
        }

        public Long getReviewId()        { return reviewId; }
        public void setReviewId(Long id) { this.reviewId = id; }
        public Long getMemberId()        { return memberId; }
        public Long getRecipeId()        { return recipeId; }
        public int getRating()           { return rating; }
        public String getContent()       { return content; }
    }
}