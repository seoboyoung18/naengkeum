package com.fridgefamer.dto.response.auth;

/**
 * AuthService 내부 전용 조회 결과 — login 시 비밀번호 해시 비교 + 탈퇴 여부 확인용.
 *
 * <p>API 응답에 직접 노출되지 않으며, password 해시를 담는다.
 * Controller나 클라이언트로 전달하지 말 것.</p>
 */
public record MemberAuthRow(
        Long memberId,
        String email,
        String password,
        String nickname,
        boolean isActive
) {}
