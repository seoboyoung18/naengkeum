package com.fridgefamer.dto.response.follow;

/**
 * 팔로우 등록/해제 응답 — API 명세 §7(팔로우) F16.
 *
 * <pre>{ "following": true }  // 등록 시 true, 해제 시 false</pre>
 *
 * <p>followerCount는 대상 유저의 현재 팔로워 수(토글 후) — 프론트가 즉시 갱신하도록 함께 반환.</p>
 */
public record FollowToggleResponse(
        boolean following,
        int followerCount
) {}