package com.fridgefamer.dto.response.review;

import java.util.List;

/**
 * 리뷰 목록 응답 — API 명세 §5 GET /api/review.
 *
 * <pre>
 * { "content":[...], "totalElements":12, "ratingStats":{ "avg":4.3, "dist":{...} } }
 * </pre>
 *
 * <p>공통 PageResponse 대신 별도 record를 쓰는 이유: 명세상 ratingStats 메타가
 * 추가로 필요하고, PageResponse를 확장하지 말라는 컨벤션(common 주석)에 따른다.
 * page/size/totalPages도 함께 제공해 페이징 일관성 유지.</p>
 */
public record ReviewListResponse(
        List<ReviewItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        RatingStats ratingStats
) {}