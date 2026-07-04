package com.fridgefamer.dto.response.admin;

/**
 * 관리자 대시보드 통계 — GET /api/admin/stats.
 *
 * @param pendingReports 미처리(PENDING) 신고가 달린 콘텐츠 수(신고 탭 목록 길이와 동일).
 */
public record AdminStats(
        long totalMembers,
        long totalRecipes,
        long totalReviews,
        long activeChallengeParticipants,
        long pendingReports
) {}