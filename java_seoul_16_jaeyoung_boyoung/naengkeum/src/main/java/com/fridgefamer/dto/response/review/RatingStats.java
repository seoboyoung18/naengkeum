package com.fridgefamer.dto.response.review;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 레시피 평점 통계 — 리뷰 목록 응답에 동봉.
 *
 * <pre>{ "avg": 4.3, "dist": { "5":9, "4":2, "3":1, "2":0, "1":0 } }</pre>
 *
 * <p>dist 키는 평점(5~1) 문자열, 값은 해당 평점 개수. 리뷰가 없으면 avg=0, 전 구간 0.</p>
 */
public record RatingStats(
        BigDecimal avg,
        Map<String, Integer> dist
) {}
