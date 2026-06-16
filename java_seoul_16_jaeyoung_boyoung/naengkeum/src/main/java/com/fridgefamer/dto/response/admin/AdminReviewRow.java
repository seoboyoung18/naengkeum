package com.fridgefamer.dto.response.admin;

import java.time.LocalDateTime;

/**
 * 관리자 리뷰 목록 행 — GET /api/admin/reviews.
 *
 * <p>신고 누적순(reportCount DESC) 정렬. 삭제는 기존 DELETE /api/review/{id}를
 * 관리자가 그대로 사용한다(verifyOwner가 관리자 허용).</p>
 *
 * @param recipeId    리뷰가 달린 레시피(클릭 시 상세 이동용)
 * @param reportCount 신고 누적 수
 */
public record AdminReviewRow(
        Long reviewId,
        Long recipeId,
        String recipeTitle,
        String authorNickname,
        int rating,
        String content,
        long reportCount,
        LocalDateTime createdAt
) {}
