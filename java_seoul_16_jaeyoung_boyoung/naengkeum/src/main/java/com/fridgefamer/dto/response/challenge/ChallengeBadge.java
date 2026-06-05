package com.fridgefamer.dto.response.challenge;

/**
 * 챌린지 카드/상세에 중첩되는 배지 정보 — API 명세 §8.
 *
 * <pre>{ "badgeId": 1, "name": "식비 0원", "iconUrl": "..." }</pre>
 */
public record ChallengeBadge(
        Long badgeId,
        String name,
        String iconUrl
) {}