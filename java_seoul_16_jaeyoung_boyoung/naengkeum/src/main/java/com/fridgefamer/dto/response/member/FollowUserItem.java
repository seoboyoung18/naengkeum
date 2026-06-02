package com.fridgefamer.dto.response.member;

/**
 * /api/member/me/following · /api/member/me/followers 응답 원소.
 *
 * <p>isFollowing은 "로그인한 본인이 이 유저를 팔로우하는지" 여부.
 * - /me/following 목록: 본인이 이미 팔로우하는 유저들이라 항상 true.
 * - /me/followers 목록: 맞팔(follow back) 여부를 표시.</p>
 */
public record FollowUserItem(
        Long memberId,
        String nickname,
        int reviewCount,
        boolean isFollowing
) {}
