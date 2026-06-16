package com.fridgefamer.dto.response.admin;

import java.time.LocalDateTime;

/**
 * 관리자 신고 목록 행 — GET /api/admin/reports.
 *
 * <p>신고 대상(레시피/리뷰)별로 묶은(GROUP BY) 미처리(PENDING) 신고. 신고 누적순.
 * 콘텐츠 클릭 시 recipeId로 레시피 상세로 이동하고, 부적절하면 삭제(레시피=recipeId,
 * 리뷰=reviewId) 또는 무시(resolve)한다.</p>
 *
 * @param targetType     "RECIPE" 또는 "REVIEW"
 * @param recipeId       이동 대상 레시피(리뷰도 소속 레시피로 이동)
 * @param reviewId       REVIEW일 때 삭제 대상 리뷰 ID, RECIPE면 null
 * @param title          레시피 제목 또는 리뷰 내용(프론트에서 말줄임)
 * @param reasons        신고 사유 모음(중복 제거, ", " 구분). 사유 미입력 신고만 있으면 null
 * @param reportCount    미처리 신고 누적 수
 * @param lastReportedAt 가장 최근 신고 시각
 */
public record AdminReportRow(
        String targetType,
        Long recipeId,
        Long reviewId,
        String title,
        String reasons,
        long reportCount,
        LocalDateTime lastReportedAt
) {}
