package com.fridgefamer.controller;

import com.fridgefamer.dto.request.admin.UserActiveRequest;
import com.fridgefamer.dto.request.admin.UserRoleRequest;
import com.fridgefamer.dto.response.admin.AdminRecipeRow;
import com.fridgefamer.dto.response.admin.AdminReportRow;
import com.fridgefamer.dto.response.admin.AdminReviewRow;
import com.fridgefamer.dto.response.admin.AdminStats;
import com.fridgefamer.dto.response.admin.AdminUserRow;
import com.fridgefamer.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 API — /api/admin/**.
 *
 * <p>SecurityConfig에서 이 경로 전체를 ROLE_ADMIN으로 보호하므로,
 * 일반 사용자/미인증은 컨트롤러 진입 전에 401/403으로 차단된다.
 * 레시피/리뷰 목록은 신고 누적순으로 노출하며, 삭제는 레시피=DELETE /api/recipe/{id},
 * 리뷰=DELETE /api/review/{id}(둘 다 관리자 허용)를 사용한다.</p>
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 대시보드 통계 (사용자/레시피/리뷰/챌린지 참여 수). */
    @GetMapping("/stats")
    public AdminStats stats() {
        return adminService.stats();
    }

    /** 전체 사용자 목록 (활성/차단 모두). keyword로 닉네임/이메일 검색. */
    @GetMapping("/users")
    public List<AdminUserRow> users(@RequestParam(required = false) String keyword) {
        return adminService.listUsers(keyword);
    }

    /** 레시피 목록 (사용자 레시피 + 신고 달린 공공 레시피, 신고 누적순). keyword로 제목 검색. */
    @GetMapping("/recipes")
    public List<AdminRecipeRow> recipes(@RequestParam(required = false) String keyword) {
        return adminService.listRecipes(keyword);
    }

    /** 전체 리뷰 목록 (신고 누적순). 삭제는 기존 DELETE /api/review/{id}를 관리자가 사용. */
    @GetMapping("/reviews")
    public List<AdminReviewRow> reviews() {
        return adminService.listReviews();
    }

    /** 미처리 신고 목록 (대상별 묶음, 신고 누적순). 신고 탭. */
    @GetMapping("/reports")
    public List<AdminReportRow> reports() {
        return adminService.listReports();
    }

    /** "무시" — 레시피의 미처리 신고를 모두 처리완료로. */
    @PatchMapping("/reports/recipe/{recipeId}/resolve")
    public Map<String, String> resolveRecipeReports(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        adminService.resolveRecipeReports(recipeId);
        return Map.of("message", "신고를 처리했습니다");
    }

    /** "무시" — 리뷰의 미처리 신고를 모두 처리완료로. */
    @PatchMapping("/reports/review/{reviewId}/resolve")
    public Map<String, String> resolveReviewReports(
            @PathVariable @Positive(message = "reviewId는 양수여야 합니다") Long reviewId
    ) {
        adminService.resolveReviewReports(reviewId);
        return Map.of("message", "신고를 처리했습니다");
    }

    /** 사용자 차단/해제. body { "active": false } = 차단. */
    @PatchMapping("/users/{memberId}/active")
    public void setUserActive(
            @PathVariable @Positive(message = "memberId는 양수여야 합니다") Long memberId,
            @Valid @RequestBody UserActiveRequest req
    ) {
        adminService.setUserActive(memberId, req.active());
    }

    /** 회원 삭제(hard delete). 관리자 계정은 삭제 불가(400). 개인 데이터는 CASCADE, 작성 레시피는 익명 보존. */
    @DeleteMapping("/users/{memberId}")
    public Map<String, String> deleteUser(
            @PathVariable @Positive(message = "memberId는 양수여야 합니다") Long memberId
    ) {
        adminService.deleteUser(memberId);
        return Map.of("message", "회원을 삭제했습니다");
    }

    /** 회원 역할 변경(USER↔ADMIN). 본인 계정의 관리자 권한 해제는 불가(400). */
    @PatchMapping("/users/{memberId}/role")
    public Map<String, String> setUserRole(
            @PathVariable @Positive(message = "memberId는 양수여야 합니다") Long memberId,
            @Valid @RequestBody UserRoleRequest req
    ) {
        adminService.setUserRole(memberId, req.role(), currentMemberId());
        return Map.of("message", "역할을 변경했습니다");
    }

    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}