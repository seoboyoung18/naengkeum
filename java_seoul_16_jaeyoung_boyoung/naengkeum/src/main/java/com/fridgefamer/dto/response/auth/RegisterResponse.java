package com.fridgefamer.dto.response.auth;

/** POST /api/auth/register 201 응답 — 생성된 회원 PK만 반환. */
public record RegisterResponse(Long memberId) {}
