package com.fridgefamer.controller;

import com.fridgefamer.dto.request.admin.UserActiveRequest;
import com.fridgefamer.dto.response.admin.AdminStats;
import com.fridgefamer.dto.response.admin.AdminUserRow;
import com.fridgefamer.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 전용 API — /api/admin/**.
 *
 * <p>SecurityConfig에서 이 경로 전체를 ROLE_ADMIN으로 보호하므로,
 * 일반 사용자/미인증은 컨트롤러 진입 전에 401/403으로 차단된다.
 * (신고 기능 없음 — 콘텐츠 삭제는 기존 DELETE를 관리자가 그대로 사용)</p>
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

    /** 전체 사용자 목록 (활성/차단 모두). */
    @GetMapping("/users")
    public List<AdminUserRow> users() {
        return adminService.listUsers();
    }

    /** 사용자 차단/해제. body { "active": false } = 차단. */
    @PatchMapping("/users/{memberId}/active")
    public void setUserActive(
            @PathVariable @Positive(message = "memberId는 양수여야 합니다") Long memberId,
            @Valid @RequestBody UserActiveRequest req
    ) {
        adminService.setUserActive(memberId, req.active());
    }
}