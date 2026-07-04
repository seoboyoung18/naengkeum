package com.fridgefamer.mapper.review;

import com.fridgefamer.dto.response.review.RatingCount;
import com.fridgefamer.dto.response.review.ReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 리뷰(review) Mapper — Review 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/review/ReviewMapper.xml.
 * isOwner는 null-safe 비교(member_id &lt;=&gt; viewerId)로 계산해 미인증 시 false.
 * 중복 작성은 UNIQUE(member_id,recipe_id) → DuplicateKeyException → 409로 자동 변환.</p>
 */
@Mapper
public interface ReviewMapper {

    // ---- 조회 ----

    /** 레시피 리뷰 목록(최신순, 페이징). viewerId nullable. */
    List<ReviewItem> selectByRecipe(@Param("recipeId") Long recipeId,
                                    @Param("viewerId") Long viewerId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    /** 레시피 리뷰 총 개수(페이징 totalElements용). */
    long countByRecipe(@Param("recipeId") Long recipeId);

    /** 평점별 개수(1~5). 비어있는 평점은 행이 없을 수 있어 서비스에서 0 채움. */
    List<RatingCount> selectRatingDist(@Param("recipeId") Long recipeId);

    /** 단건 조회(응답 생성용). 없으면 null. */
    ReviewItem selectById(@Param("reviewId") Long reviewId,
                          @Param("viewerId") Long viewerId);

    /** 소유자 member_id만 조회 — 권한 검증용. 없으면 null. */
    Long findOwnerId(@Param("reviewId") Long reviewId);

    // ---- 변경 ----

    int insertReview(@Param("cmd") ReviewCommand cmd);

    int updateReview(@Param("reviewId") Long reviewId,
                     @Param("rating") int rating,
                     @Param("content") String content);

    int deleteById(@Param("reviewId") Long reviewId);

    // =================================================================
    //  insert 후 생성 키 회수용 (record는 immutable이라 별도 클래스)
    // =================================================================
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
