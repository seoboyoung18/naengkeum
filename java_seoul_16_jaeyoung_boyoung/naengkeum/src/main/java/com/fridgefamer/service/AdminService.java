package com.fridgefamer.service;

import com.fridgefamer.dto.response.admin.AdminRecipeRow;
import com.fridgefamer.dto.response.admin.AdminReviewRow;
import com.fridgefamer.dto.response.admin.AdminStats;
import com.fridgefamer.dto.response.admin.AdminUserRow;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.admin.AdminMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 전용 서비스. /api/admin/** 는 SecurityConfig에서 ROLE_ADMIN으로 보호되므로
 * 이 서비스의 메서드는 호출 시점에 이미 관리자임이 보장된다.
 *
 * <p>사용자 관리(목록/검색/차단) + 대시보드 통계 + 신고 누적 콘텐츠(레시피/리뷰) 목록을 담당.
 * 콘텐츠 삭제는 기존 DELETE(레시피/리뷰)를 관리자가 그대로 사용한다(권한 검증이 관리자 허용).</p>
 */
@Service
public class AdminService {

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
                adminMapper.countActiveChallengeParticipants()
        );
    }
}