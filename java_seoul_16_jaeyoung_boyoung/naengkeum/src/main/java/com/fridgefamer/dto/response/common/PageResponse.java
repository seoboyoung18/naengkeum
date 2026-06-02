package com.fridgefamer.dto.response.common;

import java.util.List;

/**
 * 페이징 공통 응답 — API 명세 2026-05-29 표준화 결정.
 *
 * <pre>
 * { "content": [...], "page": 0, "size": 12, "totalElements": 120, "totalPages": 10 }
 * </pre>
 *
 * <p>메타데이터(ratingStats 등)가 필요한 엔드포인트는 별도 record로 감싸거나
 * 이 record를 확장하지 말고 같은 레벨에 추가 응답 DTO를 만들어 사용.</p>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
