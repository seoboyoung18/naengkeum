package com.fridgefamer.dto.response.review;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 평점 통계 — API 명세 §5 리뷰 목록 응답의 ratingStats.
 *
 * <pre>{ "avg": 4.3, "dist": {"5":9,"4":2,"3":1} }</pre>
 *
 * <p>dist는 평점(1~5)별 개수. 없는 평점은 0으로 채워 5개 키를 항상 제공.</p>
 */
public record RatingStats(
        BigDecimal avg,
        Map<String, Long> dist
) {}