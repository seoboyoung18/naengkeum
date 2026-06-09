package com.fridgefamer.dto.response.challenge;

/**
 * 챌린지 진행률 갱신 응답 — PATCH /api/challenge/{id}/progress (F18).
 *
 * <pre>{ "progress": 100, "achieved": true, "badgeEarned": true }</pre>
 *
 * @param progress    갱신된 진행률(0~100)
 * @param achieved    달성 여부(progress >= 100)
 * @param badgeEarned 이번 호출로 배지를 새로 획득했는지(이미 보유했으면 false).
 *                    프론트가 true일 때만 "배지 획득!" 토스트를 띄우면 된다.
 */
public record ChallengeProgressResponse(
        int progress,
        boolean achieved,
        boolean badgeEarned
) {}