package com.fridgefamer.dto.request.member;

import jakarta.validation.constraints.NotBlank;

/** DELETE /api/member/me 요청 Body — 본인 확인용 비밀번호. */
public record DeleteMemberRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {}
