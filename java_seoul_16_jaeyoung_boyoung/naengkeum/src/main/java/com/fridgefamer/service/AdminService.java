package com.fridgefamer.service;

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
 * <p>신고 기능은 두지 않는다(팀 합의). 관리자는 콘텐츠를 직접 보고 기존 DELETE로
 * 삭제하며(verifyOwner가 관리자를 허용), 여기서는 사용자 관리 + 대시보드 통계만 담당.</p>
 */
@Service
public class AdminService {

    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    /** 전체 사용자 목록 (활성/차단 모두). */
    public List<AdminUserRow> listUsers() {
        return adminMapper.selectAllUsers();
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