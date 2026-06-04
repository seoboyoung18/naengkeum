package com.fridgefamer.service;

import com.fridgefamer.dto.request.review.CreateReviewRequest;
import com.fridgefamer.dto.request.review.UpdateReviewRequest;
import com.fridgefamer.dto.response.review.RatingStats;
import com.fridgefamer.dto.response.review.ReviewItem;
import com.fridgefamer.dto.response.review.ReviewListResponse;
import com.fridgefamer.dto.response.review.ReviewRow;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.review.ReviewMapper;
import com.fridgefamer.mapper.review.ReviewMapper.RatingCountRow;
import com.fridgefamer.mapper.review.ReviewMapper.ReviewCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 리뷰(Review) 도메인 서비스 — API 명세 §5.
 *
 * <p>목록 조회는 공개(nullable viewerId, isOwner 계산용). 작성/수정/삭제는 인증 필요.
 * 작성 시 중복(409)·레시피 부재(404) 검증, 수정/삭제 시 소유자 검증(403/404)을 한다.</p>
 */
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    // =====================================================================
    //  GET /api/review — 목록 + 통계
    // =====================================================================
    public ReviewListResponse list(Long viewerId, Long recipeId, int page, int size) {
        int offset = page * size;
        List<ReviewRow> rows = reviewMapper.selectByRecipe(recipeId, offset, size);
        long total = reviewMapper.countByRecipe(recipeId);

        List<ReviewItem> content = new ArrayList<>(rows.size());
        for (ReviewRow row : rows) {
            boolean isOwner = viewerId != null && viewerId.equals(row.memberId());
            content.add(new ReviewItem(
                    row.reviewId(), row.memberId(), row.nickname(),
                    row.rating(), row.content(), isOwner, row.createdAt()));
        }

        RatingStats stats = buildRatingStats(recipeId);
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new ReviewListResponse(content, page, size, total, totalPages, stats);
    }

    // =====================================================================
    //  POST /api/review — 작성
    // =====================================================================
    @Transactional
    public ReviewItem create(Long memberId, CreateReviewRequest req) {
        // 대상 레시피 존재 검증
        if (!reviewMapper.recipeExists(req.recipeId())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        // 중복 작성 사전 체크 (DB UNIQUE도 막지만 명확한 409 위해)
        if (reviewMapper.existsByMemberAndRecipe(memberId, req.recipeId())) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 이 레시피에 리뷰를 작성했습니다");
        }

        ReviewCommand cmd = new ReviewCommand(memberId, req.recipeId(), req.rating(), req.content());
        reviewMapper.insertReview(cmd);
        return reviewMapper.selectItemById(cmd.getReviewId(), memberId);
    }

    // =====================================================================
    //  PUT /api/review/{reviewId} — 수정
    // =====================================================================
    @Transactional
    public ReviewItem update(Long memberId, Long reviewId, UpdateReviewRequest req) {
        verifyAuthor(memberId, reviewId);
        reviewMapper.updateReview(reviewId, req.rating(), req.content());
        return reviewMapper.selectItemById(reviewId, memberId);
    }

    // =====================================================================
    //  DELETE /api/review/{reviewId} — 삭제
    // =====================================================================
    @Transactional
    public void delete(Long memberId, Long reviewId) {
        verifyAuthor(memberId, reviewId);
        reviewMapper.deleteById(reviewId);
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    /** 리뷰가 존재하고 호출자 소유인지 검증. 없으면 404, 타인 것이면 403. */
    private void verifyAuthor(Long memberId, Long reviewId) {
        Long authorId = reviewMapper.findAuthorId(reviewId);
        if (authorId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다");
        }
        if (!authorId.equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 리뷰만 수정/삭제할 수 있습니다");
        }
    }

    /** 평점 통계 조립 — avg(소수1) + dist(1~5 전부 키 보장, 없으면 0). */
    private RatingStats buildRatingStats(Long recipeId) {
        List<RatingCountRow> rows = reviewMapper.selectRatingDistribution(recipeId);

        // 1~5 키를 항상 제공 (높은 점수부터 표시). 없는 평점은 0.
        Map<String, Long> dist = new LinkedHashMap<>();
        for (int score = 5; score >= 1; score--) {
            dist.put(String.valueOf(score), 0L);
        }

        long totalCount = 0;
        long weightedSum = 0;
        for (RatingCountRow row : rows) {
            dist.put(String.valueOf(row.rating()), row.count());
            totalCount += row.count();
            weightedSum += (long) row.rating() * row.count();
        }

        BigDecimal avg = (totalCount == 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(weightedSum)
                        .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);

        return new RatingStats(avg, dist);
    }
}