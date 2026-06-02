package com.fridgefamer.dto.response.member;

/**
 * MemberService 내부 전용 — 마이페이지/탈퇴/수정 흐름에서 회원 기본 정보를 한 번에 조회할 때 사용.
 *
 * <p>API 응답에 직접 노출되지 않음(비밀번호 해시 포함). Service에서 가공 후 다른 응답 DTO로 변환.</p>
 */
public record MemberBasicRow(
        Long memberId,
        String email,
        String password,
        String nickname,
        String allergies,
        boolean isActive
) {}
