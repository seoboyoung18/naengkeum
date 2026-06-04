package com.fridgefamer.dto.response.review;

import java.time.LocalDateTime;

/**
 * 리뷰 단건 응답 — API 명세 §5 (목록/작성/수정 공통).
 *
 * <pre>
 * { "reviewId":10, "memberId":3, "nickname":"닉네임", "rating":4,
 *   "content":"맛있어요!", "isOwner":false, "createdAt":"2026-05-14T10:30:00" }
 * </pre>
 *
 * <p>isOwner는 조회자(memberId) 기준 계산값 — 비로그인 시 항상 false.
 * nickname은 member JOIN으로 가져온다.</p>
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