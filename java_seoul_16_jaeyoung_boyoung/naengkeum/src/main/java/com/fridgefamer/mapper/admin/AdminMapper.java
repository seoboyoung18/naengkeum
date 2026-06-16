package com.fridgefamer.mapper.admin;

import com.fridgefamer.dto.response.admin.AdminRecipeRow;
import com.fridgefamer.dto.response.admin.AdminReportRow;
import com.fridgefamer.dto.response.admin.AdminReviewRow;
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

    /**
     * 전체 사용자 목록(활성/차단 모두). 최신 가입 순.
     * @param keyword 닉네임/이메일 부분일치 검색어. null/빈값이면 전체.
     */
    List<AdminUserRow> selectAllUsers(@Param("keyword") String keyword);

    /**
     * 관리자 레시피 목록 — 사용자가 등록한 레시피(author_id IS NOT NULL)만.
     * 식약처 공공 레시피는 제외. 신고 누적순(미처리 reportCount DESC) 정렬.
     * @param keyword 제목 부분일치 검색어. null/빈값이면 전체.
     */
    List<AdminRecipeRow> selectAdminRecipes(@Param("keyword") String keyword);

    /** 전체 리뷰 목록 — 미처리 신고 누적순 정렬. */
    List<AdminReviewRow> selectAllReviews();

    /** 신고(PENDING) 목록 — 대상(레시피/리뷰)별로 묶어 신고 누적순. */
    List<AdminReportRow> selectPendingReports();

    /** 미처리 신고가 달린 콘텐츠 수(대시보드 카드/탭 배지용). */
    long countPendingReports();

    /** 특정 레시피의 미처리 신고를 모두 처리완료(RESOLVED)로 — "무시". @return 갱신 행 수. */
    int resolveRecipeReports(@Param("recipeId") Long recipeId);

    /** 특정 리뷰의 미처리 신고를 모두 처리완료(RESOLVED)로 — "무시". @return 갱신 행 수. */
    int resolveReviewReports(@Param("reviewId") Long reviewId);

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