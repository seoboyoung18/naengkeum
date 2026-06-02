package com.fridgefamer.dto.response.member;

import java.util.List;

/**
 * GET /api/member/me 200 응답 (마이페이지).
 *
 * <p>email은 마스킹된 상태로 응답 (API 명세 예시: te***@email.com).
 * allergies는 콤마 구분 문자열을 List<String>으로 변환한 값.</p>
 */
public record MyPageResponse(
        Long memberId,
        String nickname,
        String email,
        List<String> allergies,
        MyStats stats
) {
    /** 마이페이지 통계 — 5종 카운트. */
    public record MyStats(
            int fridgeCount,
            int reviewCount,
            int wishlistCount,
            int followerCount,
            int followingCount
    ) {}
}
