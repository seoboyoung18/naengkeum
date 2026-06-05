package com.fridgefamer.dto.response.challenge;

import java.time.LocalDate;

/**
 * 챌린지 카드 — API 명세 §8 (목록 GET /api/challenge, 상세 GET /{id}, 내챌린지 /my).
 *
 * <pre>
 * { "challengeId":1, "title":"식비 0원 코스", "description":"...",
 *   "startDate":"2026-05-13", "endDate":"2026-05-19", "dDay":3,
 *   "participantCount":142, "badge":{...},
 *   "myStatus":"JOINED|NONE", "myProgress":40 }
 * </pre>
 *
 * <p>dDay = endDate - 오늘 (음수면 종료된 챌린지). myStatus/myProgress는 로그인 시에만
 * 의미 있고, 비로그인이면 myStatus="NONE", myProgress=0. badge는 중첩 객체.</p>
 */
public record ChallengeItem(
        Long challengeId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        long dDay,
        int participantCount,
        ChallengeBadge badge,
        String myStatus,        // "JOINED" | "NONE"
        int myProgress          // 0~100
) {}