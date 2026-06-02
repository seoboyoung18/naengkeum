package com.fridgefamer.dto.response.auth;

/**
 * GET /api/auth/verify 200 응답 — 토큰 유효 시 {valid:true}.
 * 유효하지 않으면 SecurityFilter가 401 응답을 만든다(이 DTO는 200 케이스 전용).
 */
public record TokenVerifyResponse(boolean valid) {}
