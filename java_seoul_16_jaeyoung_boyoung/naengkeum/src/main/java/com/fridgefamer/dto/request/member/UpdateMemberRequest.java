package com.fridgefamer.dto.request.member;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/member/me 요청 Body.
 *
 * <p>모든 필드 선택적(partial update). 비밀번호 변경 시에만 currentPassword/newPassword 모두 필수.
 * 한 필드라도 들어오면 그 필드만 갱신한다. 비즈니스 검증은 MemberService.</p>
 */
public record UpdateMemberRequest(
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다")
        String nickname,

        String currentPassword,

        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다"
        )
        String newPassword,

        List<String> allergies
) {}
