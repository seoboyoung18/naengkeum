package com.fridgefamer.dto.request.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 콘텐츠 신고 요청 — POST /api/report.
 *
 * @param targetType "RECIPE" 또는 "REVIEW"
 * @param targetId   신고 대상 ID (recipeId 또는 reviewId)
 * @param reason     신고 사유(선택, 255자 이내)
 */
public record CreateReportRequest(
        @NotNull(message = "targetType은 필수입니다")
        @Pattern(regexp = "RECIPE|REVIEW", message = "targetType은 RECIPE 또는 REVIEW여야 합니다")
        String targetType,

        @NotNull(message = "targetId는 필수입니다")
        @Positive(message = "targetId는 양수여야 합니다")
        Long targetId,

        @Size(max = 255, message = "신고 사유는 255자 이내여야 합니다")
        String reason
) {}
