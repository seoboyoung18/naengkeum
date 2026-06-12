package com.fridgefamer.dto.response.admin;

/**
 * 관리자 사용자 목록 행 — GET /api/admin/users.
 *
 * @param active true=활성, false=차단됨(is_active=0)
 */
public record AdminUserRow(
        Long memberId,
        String email,
        String nickname,
        String role,
        boolean active
) {}