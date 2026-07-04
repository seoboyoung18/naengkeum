package com.fridgefamer.controller;

import com.fridgefamer.dto.response.follow.FollowToggleResponse;
import com.fridgefamer.service.FollowService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팔로우(Follow) API — API 명세 §7 (F16). 전부 인증 필요.
 *
 * <ul>
 *   <li>POST   /api/follow/{followeeId} — 팔로우 등록 (자기 400 / 대상없음 404 / 중복 409)</li>
 *   <li>DELETE /api/follow/{followeeId} — 팔로우 해제</li>
 * </ul>
 *
 * <p>팔로잉/팔로워 목록 조회는 MemberController(/api/member/me/following·followers)에 있음.</p>
 */
@RestController
@RequestMapping("/api/follow")
@Validated
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{followeeId}")
    public FollowToggleResponse follow(
            @PathVariable @Positive(message = "followeeId는 양수여야 합니다") Long followeeId
    ) {
        return followService.follow(currentMemberId(), followeeId);
    }

    @DeleteMapping("/{followeeId}")
    public FollowToggleResponse unfollow(
            @PathVariable @Positive(message = "followeeId는 양수여야 합니다") Long followeeId
    ) {
        return followService.unfollow(currentMemberId(), followeeId);
    }

    /** 인증 필수 — Security 필터 통과 시 principal은 Long. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}