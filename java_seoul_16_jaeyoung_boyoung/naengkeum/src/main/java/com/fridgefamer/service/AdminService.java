package com.fridgefamer.service;

import com.fridgefamer.dto.response.admin.AdminRecipeRow;
import com.fridgefamer.dto.response.admin.AdminReportRow;
import com.fridgefamer.dto.response.admin.AdminReviewRow;
import com.fridgefamer.dto.response.admin.AdminStats;
import com.fridgefamer.dto.response.admin.AdminUserRow;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.admin.AdminMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 관리자 전용 서비스. /api/admin/** 는 SecurityConfig에서 ROLE_ADMIN으로 보호되므로
 * 이 서비스의 메서드는 호출 시점에 이미 관리자임이 보장된다.
 *
 * <p>사용자 관리(목록/검색/차단) + 대시보드 통계 + 신고 누적 콘텐츠(레시피/리뷰) 목록을 담당.
 * 콘텐츠 삭제는 기존 DELETE(레시피/리뷰)를 관리자가 그대로 사용한다(권한 검증이 관리자 허용).</p>
 */
@Service
public class AdminService {

    /** 운영자(원조 관리자) 이메일 — V9/V14에서 ADMIN으로 지정한 계정과 동일.
     *  관리자 "강등(ADMIN→USER)" 권한은 이 계정들만 보유한다. 운영자 변경 시 함께 갱신. */
    private static final Set<String> SUPER_ADMIN_EMAILS =
            Set.of("danna0326@naver.com", "gaza1268@naver.com");

    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    /** 전체 사용자 목록 (활성/차단 모두). keyword로 닉네임/이메일 검색(null이면 전체). */
    public List<AdminUserRow> listUsers(String keyword) {
        return adminMapper.selectAllUsers(trimToNull(keyword));
    }

    /** 관리자 레시피 목록 (사용자 레시피 + 신고 달린 공공 레시피, 신고 누적순). */
    public List<AdminRecipeRow> listRecipes(String keyword) {
        return adminMapper.selectAdminRecipes(trimToNull(keyword));
    }

    /** 전체 리뷰 목록 (신고 누적순). */
    public List<AdminReviewRow> listReviews() {
        return adminMapper.selectAllReviews();
    }

    /** 미처리 신고 목록 (대상별 묶음, 신고 누적순). */
    public List<AdminReportRow> listReports() {
        return adminMapper.selectPendingReports();
    }

    /** "무시" — 레시피의 미처리 신고를 처리완료로. */
    @Transactional
    public void resolveRecipeReports(Long recipeId) {
        adminMapper.resolveRecipeReports(recipeId);
    }

    /** "무시" — 리뷰의 미처리 신고를 처리완료로. */
    @Transactional
    public void resolveReviewReports(Long reviewId) {
        adminMapper.resolveReviewReports(reviewId);
    }

    /**
     * 사용자 차단/해제. active=false면 차단(is_active=0), true면 해제.
     * 안전장치: 관리자 계정은 차단 불가(서로 잠그는 사고 방지).
     */
    @Transactional
    public void setUserActive(Long targetMemberId, boolean active) {
        String role = adminMapper.selectRole(targetMemberId);
        if (role == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다");
        }
        if (!active && "ADMIN".equals(role)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "관리자 계정은 차단할 수 없습니다");
        }
        adminMapper.updateActive(targetMemberId, active);
    }

    /**
     * 회원 삭제(hard delete). 관리자 계정은 삭제 불가 — 먼저 일반 회원으로 변경해야 한다(실수 방지).
     * 개인 데이터(리뷰/냉장고/찜/팔로우/챌린지/배지/신고)는 FK CASCADE로 함께 삭제되고,
     * 작성한 레시피는 author_id=NULL로 전환되어 공공 레시피처럼 보존된다(V6 SET NULL).
     */
    @Transactional
    public void deleteUser(Long targetMemberId) {
        String role = adminMapper.selectRole(targetMemberId);
        if (role == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다");
        }
        if ("ADMIN".equals(role)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "관리자 계정은 삭제할 수 없습니다. 먼저 일반 회원으로 변경하세요");
        }
        adminMapper.deleteMember(targetMemberId);
    }

    /**
     * 회원 역할 변경(USER↔ADMIN).
     * <ul>
     *   <li>승격(USER→ADMIN): 모든 관리자가 가능.</li>
     *   <li>강등(ADMIN→USER): 운영자(원조 관리자, {@link #SUPER_ADMIN_EMAILS})만 수행 가능.
     *       본인 계정은 강등 불가(관리자 패널 접근을 스스로 잃는 사고 방지).</li>
     * </ul>
     */
    @Transactional
    public void setUserRole(Long targetMemberId, String role, Long currentMemberId) {
        String current = adminMapper.selectRole(targetMemberId);
        if (current == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다");
        }
        // 강등(→USER)만 제한: 운영자 계정만 수행 가능, 본인은 불가. 승격(→ADMIN)은 자유.
        if (!"ADMIN".equals(role)) {
            if (targetMemberId.equals(currentMemberId)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "본인 계정의 관리자 권한은 해제할 수 없습니다");
            }
            String actorEmail = adminMapper.selectEmail(currentMemberId);
            if (actorEmail == null || !SUPER_ADMIN_EMAILS.contains(actorEmail)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "관리자 강등은 운영자 계정만 할 수 있습니다");
            }
        }
        adminMapper.updateRole(targetMemberId, role);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 대시보드 통계. */
    public AdminStats stats() {
        return new AdminStats(
                adminMapper.countMembers(),
                adminMapper.countRecipes(),
                adminMapper.countReviews(),
                adminMapper.countActiveChallengeParticipants(),
                adminMapper.countPendingReports()
        );
    }
}