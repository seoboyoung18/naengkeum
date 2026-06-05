package com.fridgefamer.dto.response.challenge;

/**
 * 챌린지 참여/언조인 응답 — API 명세 §8.
 *
 * <pre>{ "joined": true }  // 참여 시 true, 언조인 시 false</pre>
 */
public record ChallengeJoinResponse(
        boolean joined
) {}