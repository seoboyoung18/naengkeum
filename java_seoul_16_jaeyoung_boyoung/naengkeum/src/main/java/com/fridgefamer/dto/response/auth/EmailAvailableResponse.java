package com.fridgefamer.dto.response.auth;

/**
 * GET /api/auth/check-email 200 응답.
 *
 * <p>API 명세 2026-05-29 결정: 중복(사용 불가)일 때도 200 + {available:false} 반환.
 * 4xx는 진짜 검증 에러일 때만(@Valid 실패).</p>
 */
public record EmailAvailableResponse(boolean available) {}
