package com.fridgefamer.dto.response.review;

import java.time.LocalDateTime;

/**
 * 리뷰 단건 응답 — API 명세 §5.
 *
 * <pre>
 * { "reviewId":10, "memberId":3, "nickname":"닉네임", "rating":4,
 *   "content":"맛있어요!", "isOwner":false, "createdAt":"2026-05-14T10:30:00" }
 * </pre>
 *
 * <p>isOwner = 조회자(viewerId)가 작성자인지. 미인증 조회 시 항상 false.</p>
 */
public record ReviewItem(
        Long reviewId,
        Long memberId,
        String nickname,
        int rating,
        String content,
        boolean isOwner,
        LocalDateTime createdAt
) {}
