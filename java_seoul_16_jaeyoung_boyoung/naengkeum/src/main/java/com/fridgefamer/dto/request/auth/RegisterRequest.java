package com.fridgefamer.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/auth/register 요청 Body.
 *
 * <p>비밀번호 정책: 영문 + 숫자 + 특수문자 1자 이상씩 포함, 8자 이상 (API 명세 §1).
 * 닉네임 정책: 2~10자 (member.nickname VARCHAR(20)이지만 UX 기준 10자 상한).</p>
 *
 * <p>allergies는 콤마 구분 문자열로 member.allergies에 저장된다(스키마 VARCHAR(255)).
 * AuthService에서 join 수행.</p>
 */
public record RegisterRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다"
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다")
        String nickname,

        List<String> allergies,

        Boolean marketingAgree
) {}
