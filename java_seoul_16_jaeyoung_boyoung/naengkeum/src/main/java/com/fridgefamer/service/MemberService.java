package com.fridgefamer.service;

import com.fridgefamer.dto.request.member.UpdateMemberRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.member.BadgeItem;
import com.fridgefamer.dto.response.member.FollowUserItem;
import com.fridgefamer.dto.response.member.MemberBasicRow;
import com.fridgefamer.dto.response.member.MyPageResponse;
import com.fridgefamer.dto.response.member.MyReviewItem;
import com.fridgefamer.dto.response.member.OtherProfileResponse;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.member.MemberMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 회원(Member) 도메인 서비스 — 마이페이지/수정/탈퇴/리뷰목록/팔로우목록/배지/타유저 프로필.
 *
 * <p>모든 "me" 흐름은 Controller가 SecurityContext에서 추출한 memberId를 받는다.
 * 클라이언트가 보낸 id를 신뢰하지 않는다(CLAUDE.md §3-6).</p>
 */
@Service
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberMapper memberMapper, PasswordEncoder passwordEncoder) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================================
    //  GET /api/member/me
    // =====================================================================
    public MyPageResponse getMyPage(Long memberId) {
        MemberBasicRow row = findActiveOrThrow(memberId);
        MyPageResponse.MyStats stats = memberMapper.selectMyStats(memberId);
        return new MyPageResponse(
                row.memberId(),
                row.nickname(),
                row.profileImageUrl(),
                maskEmail(row.email()),
                parseAllergies(row.allergies()),
                stats
        );
    }

    // =====================================================================
    //  PUT /api/member/me
    // =====================================================================
    @Transactional
    public MyPageResponse updateMe(Long memberId, UpdateMemberRequest req) {
        MemberBasicRow current = findActiveOrThrow(memberId);

        // 1. 닉네임 변경
        if (req.nickname() != null && !req.nickname().equals(current.nickname())) {
            if (memberMapper.countByNicknameExcept(req.nickname(), memberId) > 0) {
                throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다");
            }
            memberMapper.updateNickname(memberId, req.nickname());
        }

        // 2. 비밀번호 변경 — currentPassword + newPassword 모두 들어왔을 때만
        if (req.newPassword() != null) {
            if (req.currentPassword() == null || req.currentPassword().isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "비밀번호 변경 시 현재 비밀번호가 필요합니다");
            }
            if (!passwordEncoder.matches(req.currentPassword(), current.password())) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다");
            }
            memberMapper.updatePassword(memberId, passwordEncoder.encode(req.newPassword()));
        }

        // 3. 알레르기 갱신 — 명시적으로 들어왔을 때만 (null이면 무시)
        if (req.allergies() != null) {
            memberMapper.updateAllergies(memberId, serializeAllergies(req.allergies()));
        }

        return getMyPage(memberId);
    }

    // =====================================================================
    //  DELETE /api/member/me — soft delete
    // =====================================================================
    @Transactional
    public void deleteMe(Long memberId, String rawPassword) {
        MemberBasicRow current = findActiveOrThrow(memberId);
        if (!passwordEncoder.matches(rawPassword, current.password())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다");
        }
        memberMapper.softDeleteById(memberId);
    }

    // =====================================================================
    //  GET /api/member/me/reviews
    // =====================================================================
    public PageResponse<MyReviewItem> listMyReviews(Long memberId, int page, int size) {
        int offset = page * size;
        List<MyReviewItem> content = memberMapper.selectMyReviews(memberId, offset, size);
        long total = memberMapper.countReviewByMemberId(memberId);
        return PageResponse.of(content, page, size, total);
    }

    // =====================================================================
    //  GET /api/member/me/following · /me/followers
    // =====================================================================
    public List<FollowUserItem> listFollowing(Long memberId) {
        return memberMapper.selectFollowing(memberId);
    }

    public List<FollowUserItem> listFollowers(Long memberId) {
        return memberMapper.selectFollowers(memberId);
    }

    // =====================================================================
    //  GET /api/member/me/badges
    // =====================================================================
    public List<BadgeItem> listBadges(Long memberId) {
        return memberMapper.selectBadges(memberId);
    }

    // =====================================================================
    //  GET /api/member/{userId}/profile
    //  viewerId는 비로그인 시 null. isFollowing 계산용.
    // =====================================================================
    public OtherProfileResponse getOtherProfile(Long targetId, Long viewerId) {
        OtherProfileResponse profile = memberMapper.selectOtherProfile(targetId, viewerId);
        if (profile == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다");
        }
        return profile;
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    private MemberBasicRow findActiveOrThrow(Long memberId) {
        MemberBasicRow row = memberMapper.findActiveById(memberId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다");
        }
        return row;
    }

    /** "te***@email.com" 형태로 마스킹. 로컬파트 앞 2자만 노출. */
    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 2) return email; // 너무 짧으면 마스킹 의미 없음
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private List<String> parseAllergies(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String serializeAllergies(List<String> allergies) {
        if (allergies == null || allergies.isEmpty()) return null;
        return String.join(",", allergies.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList());
    }
}
