package com.fridgefamer.dto.response.admin;

/**
 * 관리자 대시보드 통계 — GET /api/admin/stats.
 */
public record AdminStats(
        long totalMembers,
        long totalRecipes,
        long totalReviews,
        long activeChallengeParticipants
) {}