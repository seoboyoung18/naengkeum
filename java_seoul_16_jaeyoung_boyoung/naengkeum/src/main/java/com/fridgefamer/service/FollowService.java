package com.fridgefamer.service;

import com.fridgefamer.dto.response.follow.FollowToggleResponse;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.follow.FollowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팔로우(Follow) 도메인 서비스 — API 명세 §7 (F16). 전부 인증 필요.
 *
 * <p>등록/해제 액션만 담당(조회 목록은 MemberService에 있음). 검증 순서:
 * 자기 자신(400) → 대상 존재(404) → 중복(409, 등록 시).</p>
 */
@Service
public class FollowService {

    private final FollowMapper followMapper;

    public FollowService(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    // =====================================================================
    //  POST /api/follow/{followeeId} — 팔로우 등록
    // =====================================================================
    @Transactional
    public FollowToggleResponse follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "자기 자신은 팔로우할 수 없습니다");
        }
        if (!followMapper.memberExists(followeeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "대상 회원을 찾을 수 없습니다");
        }
        if (followMapper.exists(followerId, followeeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 팔로우한 회원입니다");
        }
        followMapper.insertFollow(followerId, followeeId);
        return new FollowToggleResponse(true, followMapper.countFollowers(followeeId));
    }

    // =====================================================================
    //  DELETE /api/follow/{followeeId} — 팔로우 해제
    // =====================================================================
    @Transactional
    public FollowToggleResponse unfollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "자기 자신은 팔로우할 수 없습니다");
        }
        // 멱등 처리: 팔로우 안 한 상태에서 해제해도 결과는 "팔로우 안 됨"으로 동일
        followMapper.deleteFollow(followerId, followeeId);
        return new FollowToggleResponse(false, followMapper.countFollowers(followeeId));
    }
}