package com.fridgefamer.dto.response.member;

import java.time.LocalDateTime;

/** GET /api/member/me/reviews content 원소. */
public record MyReviewItem(
        Long reviewId,
        Long recipeId,
        String recipeTitle,
        String recipeThumbnailUrl,
        int rating,
        String content,
        LocalDateTime createdAt
) {}
