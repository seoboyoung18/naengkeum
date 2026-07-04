package com.fridgefamer.dto.response.challenge;

/**
 * 챌린지 활성 사용자 통계 — API 명세 §8 GET /api/challenge/stats.
 *
 * <pre>{ "activeParticipants": 240 }</pre>
 *
 * <p>activeParticipants = 진행 중(오늘이 기간 내)인 챌린지에 참여한 (중복 제거) 회원 수.</p>
 */
public record ChallengeStatsResponse(
        long activeParticipants
) {}