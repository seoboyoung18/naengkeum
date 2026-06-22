package com.fridgefamer.dto.request.member;

/**
 * DELETE /api/member/me 요청 Body — 본인 확인용 비밀번호.
 *
 * <p>비밀번호는 선택값이다. 일반(LOCAL) 계정은 서비스에서 일치 검증을 강제하지만,
 * 소셜 로그인 전용 계정(비밀번호 없음)은 비밀번호 없이 탈퇴할 수 있어 null을 허용한다.</p>
 */
public record DeleteMemberRequest(
        String password
) {}
