package com.fridgefamer.dto.response.review;

/**
 * 평점별 개수 행 — ratingStats.dist 조립용 내부 row.
 */
public record RatingCount(
        int rating,
        int count
) {}
