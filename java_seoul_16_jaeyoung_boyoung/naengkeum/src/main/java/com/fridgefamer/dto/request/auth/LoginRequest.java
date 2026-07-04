package com.fridgefamer.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/auth/login 요청 Body.
 *
 * <p>rememberMe=true 인 경우 JwtProvider가 30일 만료 토큰을 발급한다.
 * 누락 시 false로 간주(record 기본값 처리는 AuthService에서 수행).</p>
 */
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        Boolean rememberMe
) {}
