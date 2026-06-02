package com.fridgefamer.dto.response.member;

import java.time.LocalDateTime;

/** GET /api/member/me/badges 응답 원소. */
public record BadgeItem(
        Long badgeId,
        String name,
        String iconUrl,
        LocalDateTime earnedAt
) {}
