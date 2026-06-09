package com.fridgefamer.dto.request.challenge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 챌린지 진행률 갱신 요청 — PATCH /api/challenge/{id}/progress (F18).
 *
 * <pre>{ "progress": 100 }</pre>
 *
 * <p>progress는 절대값 0~100. 100 도달 시 서비스가 달성 처리 + 배지 자동 지급.
 * (4주 PoC 단순화: 챌린지별 조건 자동 추적 대신 진행률을 직접 갱신)</p>
 */
public record ChallengeProgressRequest(
        @NotNull(message = "progress는 필수입니다")
        @Min(value = 0, message = "progress는 0 이상이어야 합니다")
        @Max(value = 100, message = "progress는 100 이하여야 합니다")
        Integer progress
) {}