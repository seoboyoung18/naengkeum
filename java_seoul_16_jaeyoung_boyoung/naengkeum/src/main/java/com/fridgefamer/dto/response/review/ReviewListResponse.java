package com.fridgefamer.dto.response.review;

import java.util.List;

/**
 * 리뷰 목록 응답 — API 명세 §5 F07.
 *
 * <p>페이징 표준(content/page/size/totalElements/totalPages)에 ratingStats를 같은 레벨로 동봉.
 * PageResponse를 확장하지 말라는 명세 결정에 따라 별도 record로 둔다.</p>
 */
public record ReviewListResponse(
        List<ReviewItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        RatingStats ratingStats
) {}
