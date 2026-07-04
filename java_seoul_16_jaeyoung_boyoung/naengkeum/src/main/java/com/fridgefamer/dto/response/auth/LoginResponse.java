package com.fridgefamer.dto.response.auth;

/**
 * POST /api/auth/login 200 응답.
 *
 * <pre>
 * { "token": "eyJ...", "nickname": "냉파왕", "memberId": 1 }
 * </pre>
 */
public record LoginResponse(
        String token,
        String nickname,
        Long memberId
) {}
