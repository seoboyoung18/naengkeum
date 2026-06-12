package com.fridgefamer.mapper.admin;

import com.fridgefamer.dto.response.admin.AdminUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 관리자 전용 조회/조작 매퍼.
 *
 * <p>모든 메서드는 SecurityConfig에서 /api/admin/** 가 ROLE_ADMIN으로 보호되는 것을
 * 전제로 한다(컨트롤러 진입 자체가 관리자만 가능).</p>
 */
@Mapper
public interface AdminMapper {

    /** 전체 사용자 목록(활성/차단 모두). 최신 가입 순. */
    List<AdminUserRow> selectAllUsers();

    /**
     * 사용자 활성/차단 토글. is_active = 1(활성) / 0(차단).
     * @return 갱신된 행 수(0이면 없는 회원 → 404).
     */
    int updateActive(@Param("memberId") Long memberId,
                     @Param("active") boolean active);

    /** 특정 회원의 역할 조회(자기 자신/관리자 보호용). 없으면 null. */
    String selectRole(@Param("memberId") Long memberId);

    // ---- 대시보드 통계 ----
    long countMembers();
    long countRecipes();
    long countReviews();
    long countActiveChallengeParticipants();
}