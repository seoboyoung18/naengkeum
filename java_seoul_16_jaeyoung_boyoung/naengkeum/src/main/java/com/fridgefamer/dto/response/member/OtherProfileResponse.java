package com.fridgefamer.dto.response.member;

/**
 * GET /api/member/{userId}/profile 200 응답.
 *
 * <p>isFollowing은 호출자(로그인 회원)가 대상 유저를 팔로우하는지 여부.
 * 비로그인 호출 시 false.</p>
 */
public record OtherProfileResponse(
        Long memberId,
        String nickname,
        int reviewCount,
        int followerCount,
        boolean isFollowing
) {}
