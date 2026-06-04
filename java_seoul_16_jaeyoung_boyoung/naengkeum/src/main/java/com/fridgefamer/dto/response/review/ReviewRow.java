package com.fridgefamer.dto.response.review;

import java.time.LocalDateTime;

/**
 * 리뷰 목록 DB 조회 1행 — Mapper 전용 중간 DTO.
 *
 * <p>isOwner(조회자 기준 계산)를 제외한 순수 행. 서비스에서 memberId 비교 후
 * ReviewItem으로 조립한다.</p>
 */
public record ReviewRow(
        Long reviewId,
        Long memberId,
        String nickname,
        int rating,
        String content,
        LocalDateTime createdAt
) {}