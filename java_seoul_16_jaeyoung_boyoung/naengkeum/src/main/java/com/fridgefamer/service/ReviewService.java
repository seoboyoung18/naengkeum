package com.fridgefamer.service;

import com.fridgefamer.dto.request.review.CreateReviewRequest;
import com.fridgefamer.dto.request.review.UpdateReviewRequest;
import com.fridgefamer.dto.response.review.RatingCount;
import com.fridgefamer.dto.response.review.RatingStats;
import com.fridgefamer.dto.response.review.ReviewItem;
import com.fridgefamer.dto.response.review.ReviewListResponse;
import com.fridgefamer.config.SecurityUtils;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.review.ReviewMapper;
import com.fridgefamer.mapper.review.ReviewMapper.ReviewCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 리뷰(Review) 도메인 서비스 — API 명세 §5.
 *
 * <p>목록은 인증 선택(isOwner 계산용 viewerId nullable). 작성/수정/삭제는 인증 필수이며
 * 수정·삭제는 소유자 검증(없음 404, 타인 403). 중복 작성은 DB UNIQUE로 409 처리.</p>
 */
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    // =====================================================================
    //  GET /api/review?recipeId=
    // =====================================================================
    public ReviewListResponse list(Long recipeId, Long viewerId, int page, int size) {
        int offset = page * size;
        List<ReviewItem> content = reviewMapper.selectByRecipe(recipeId, viewerId, offset, size);
        long total = reviewMapper.countByRecipe(recipeId);
        RatingStats stats = buildRatingStats(reviewMapper.selectRatingDist(recipeId));
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new ReviewListResponse(content, page, size, total, totalPages, stats);
    }

    // =====================================================================
    //  POST /api/review
    // =====================================================================
    @Transactional
    public ReviewItem create(Long memberId, CreateReviewRequest req) {
        ReviewCommand cmd = new ReviewCommand(memberId, req.recipeId(), req.rating(), req.content());
        reviewMapper.insertReview(cmd);   // 중복 시 DuplicateKeyException → 409
        return reviewMapper.selectById(cmd.getReviewId(), memberId);
    }

    // =====================================================================
    //  PUT /api/review/{reviewId}
    // =====================================================================
    @Transactional
    public ReviewItem update(Long memberId, Long reviewId, UpdateReviewRequest req) {
        verifyOwner(memberId, reviewId);
        reviewMapper.updateReview(reviewId, req.rating(), req.content());
        return reviewMapper.selectById(reviewId, memberId);
    }

    // =====================================================================
    //  DELETE /api/review/{reviewId}
    // =====================================================================
    @Transactional
    public void delete(Long memberId, Long reviewId) {
        verifyOwner(memberId, reviewId);
        reviewMapper.deleteById(reviewId);
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    /** 리뷰가 존재하고 호출자 소유(또는 관리자)인지 검증. 없으면 404, 권한 없으면 403. */
    private void verifyOwner(Long memberId, Long reviewId) {
        Long ownerId = reviewMapper.findOwnerId(reviewId);
        if (ownerId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다");
        }
        // 작성자 본인 또는 관리자만 허용 (관리자는 부적절한 리뷰를 직접 삭제)
        if (!ownerId.equals(memberId) && !SecurityUtils.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 리뷰만 수정/삭제할 수 있습니다");
        }
    }

    /** 평점 분포 행 → {avg, dist(5~1, 0채움)}. 리뷰 0건이면 avg=0. */
    private RatingStats buildRatingStats(List<RatingCount> rows) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            dist.put(String.valueOf(star), 0);
        }
        long totalCount = 0;
        long weightedSum = 0;
        for (RatingCount rc : rows) {
            dist.put(String.valueOf(rc.rating()), rc.count());
            totalCount += rc.count();
            weightedSum += (long) rc.rating() * rc.count();
        }
        BigDecimal avg = totalCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(weightedSum)
                        .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
        return new RatingStats(avg, dist);
    }
}