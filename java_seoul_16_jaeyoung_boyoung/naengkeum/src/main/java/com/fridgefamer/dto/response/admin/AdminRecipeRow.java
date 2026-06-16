package com.fridgefamer.dto.response.admin;

/**
 * 관리자 레시피 목록 행 — GET /api/admin/recipes.
 *
 * <p>사용자 공개/비공개 레시피(author_id IS NOT NULL)와, 신고가 달린 공공 레시피를
 * 포함한다. 신고 누적순(reportCount DESC) 정렬이라 문제 콘텐츠가 위로 올라온다.</p>
 *
 * @param authorNickname 작성자 닉네임. 공공(식약처) 레시피는 null.
 * @param reportCount    신고 누적 수.
 */
public record AdminRecipeRow(
        Long recipeId,
        String title,
        String authorNickname,
        boolean isPublic,
        long reviewCount,
        long reportCount
) {}
