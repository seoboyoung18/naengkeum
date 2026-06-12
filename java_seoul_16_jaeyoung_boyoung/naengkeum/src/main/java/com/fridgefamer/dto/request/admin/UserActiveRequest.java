package com.fridgefamer.dto.request.admin;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자 차단/해제 요청 — PATCH /api/admin/users/{id}/active.
 *
 * <pre>{ "active": false }</pre>  // false=차단, true=해제
 */
public record UserActiveRequest(
        @NotNull(message = "active는 필수입니다")
        Boolean active
) {}