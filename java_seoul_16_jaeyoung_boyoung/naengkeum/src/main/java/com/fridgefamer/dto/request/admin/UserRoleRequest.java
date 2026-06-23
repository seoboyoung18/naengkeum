package com.fridgefamer.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 사용자 역할 변경 요청 — PATCH /api/admin/users/{id}/role.
 *
 * <pre>{ "role": "ADMIN" }</pre>  // USER 또는 ADMIN
 */
public record UserRoleRequest(
        @NotBlank(message = "role은 필수입니다")
        @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN이어야 합니다")
        String role
) {}
