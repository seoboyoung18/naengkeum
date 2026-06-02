package com.fridgefamer.controller;

import com.fridgefamer.dto.request.member.DeleteMemberRequest;
import com.fridgefamer.dto.request.member.UpdateMemberRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.member.BadgeItem;
import com.fridgefamer.dto.response.member.FollowUserItem;
import com.fridgefamer.dto.response.member.MyPageResponse;
import com.fridgefamer.dto.response.member.MyReviewItem;
import com.fridgefamer.dto.response.member.OtherProfileResponse;
import com.fridgefamer.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 회원(Member) API — API 명세 §2.
 *
 * <ul>
 *   <li>GET    /api/member/me                — F11 마이페이지 조회</li>
 *   <li>PUT    /api/member/me                — F12 회원정보 수정</li>
 *   <li>DELETE /api/member/me                — F13 회원 탈퇴 (soft)</li>
 *   <li>GET    /api/member/me/reviews        — F11 마이 리뷰 목록</li>
 *   <li>GET    /api/member/me/following      — F16 팔로잉 목록</li>
 *   <li>GET    /api/member/me/followers      — F16 팔로워 목록</li>
 *   <li>GET    /api/member/me/badges         — F18 배지 목록</li>
 *   <li>GET    /api/member/{userId}/profile  — F16 타 유저 프로필 (공개)</li>
 * </ul>
 *
 * <p>/me 하위는 인증 필요(SecurityConfig anyRequest().authenticated()).
 * /{userId}/profile은 공개 — SecurityConfig 화이트리스트 /api/member/{userId}/profile.</p>
 */
@RestController
@RequestMapping("/api/member")
@Validated
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // -----------------------------------------------------------------
    //  /me CRUD
    // -----------------------------------------------------------------

    @GetMapping("/me")
    public MyPageResponse getMe() {
        Long memberId = currentMemberId();
        return memberService.getMyPage(memberId);
    }

    @PutMapping("/me")
    public MyPageResponse updateMe(@Valid @RequestBody UpdateMemberRequest req) {
        Long memberId = currentMemberId();
        return memberService.updateMe(memberId, req);
    }

    @DeleteMapping("/me")
    public Map<String, String> deleteMe(@Valid @RequestBody DeleteMemberRequest req) {
        Long memberId = currentMemberId();
        memberService.deleteMe(memberId, req.password());
        return Map.of("message", "탈퇴 완료");
    }

    // -----------------------------------------------------------------
    //  /me/* 목록
    // -----------------------------------------------------------------

    @GetMapping("/me/reviews")
    public PageResponse<MyReviewItem> myReviews(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다") int size
    ) {
        return memberService.listMyReviews(currentMemberId(), page, size);
    }

    @GetMapping("/me/following")
    public List<FollowUserItem> myFollowing() {
        return memberService.listFollowing(currentMemberId());
    }

    @GetMapping("/me/followers")
    public List<FollowUserItem> myFollowers() {
        return memberService.listFollowers(currentMemberId());
    }

    @GetMapping("/me/badges")
    public List<BadgeItem> myBadges() {
        return memberService.listBadges(currentMemberId());
    }

    // -----------------------------------------------------------------
    //  타 유저 프로필 — 공개 (인증 선택)
    // -----------------------------------------------------------------

    @GetMapping("/{userId}/profile")
    public OtherProfileResponse otherProfile(
            @PathVariable @Positive(message = "userId는 양수여야 합니다") Long userId
    ) {
        Long viewerId = currentMemberIdOrNull();
        return memberService.getOtherProfile(userId, viewerId);
    }

    // =================================================================
    //  내부 헬퍼 — SecurityContext에서 memberId 추출
    // =================================================================

    /** 인증 필수 엔드포인트용. Security 필터를 통과했다면 principal은 Long. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    /** 인증 선택 엔드포인트용. 익명/미인증 시 null. */
    private Long currentMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Long id) ? id : null;
    }
}
