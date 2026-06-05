package com.fridgefamer.controller;

import com.fridgefamer.dto.response.challenge.ChallengeItem;
import com.fridgefamer.dto.response.challenge.ChallengeJoinResponse;
import com.fridgefamer.dto.response.challenge.ChallengeStatsResponse;
import com.fridgefamer.service.ChallengeService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 챌린지(Challenge) API — API 명세 §8 (F18).
 *
 * <ul>
 *   <li>GET    /api/challenge            — 목록 (공개, ?status=active|ended)</li>
 *   <li>GET    /api/challenge/my         — 내가 참여 중인 챌린지 (인증)</li>
 *   <li>GET    /api/challenge/stats      — 활성 사용자 통계 (공개)</li>
 *   <li>GET    /api/challenge/{id}       — 상세 (공개)</li>
 *   <li>POST   /api/challenge/{id}/join  — 참여 (201, 중복 409)</li>
 *   <li>DELETE /api/challenge/{id}/join  — 언조인 (200)</li>
 * </ul>
 *
 * <p>공개 API는 로그인 시 myStatus/myProgress를 채우기 위해 currentMemberIdOrNull 사용.
 * 고정 경로(/my, /stats)를 변수 경로(/{id})보다 먼저 선언해 매핑 모호성을 피한다.</p>
 */
@RestController
@RequestMapping("/api/challenge")
@Validated
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public List<ChallengeItem> list(
            @RequestParam(required = false)
            @Pattern(regexp = "active|ended", message = "status는 active 또는 ended여야 합니다")
            String status
    ) {
        return challengeService.list(currentMemberIdOrNull(), status);
    }

    @GetMapping("/my")
    public List<ChallengeItem> myChallenges() {
        return challengeService.myChallenges(currentMemberId());
    }

    @GetMapping("/stats")
    public ChallengeStatsResponse stats() {
        return challengeService.stats();
    }

    @GetMapping("/{challengeId}")
    public ChallengeItem detail(
            @PathVariable @Positive(message = "challengeId는 양수여야 합니다") Long challengeId
    ) {
        return challengeService.detail(currentMemberIdOrNull(), challengeId);
    }

    @PostMapping("/{challengeId}/join")
    public ResponseEntity<ChallengeJoinResponse> join(
            @PathVariable @Positive(message = "challengeId는 양수여야 합니다") Long challengeId
    ) {
        ChallengeJoinResponse res = challengeService.join(currentMemberId(), challengeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/{challengeId}/join")
    public ChallengeJoinResponse unjoin(
            @PathVariable @Positive(message = "challengeId는 양수여야 합니다") Long challengeId
    ) {
        return challengeService.unjoin(currentMemberId(), challengeId);
    }

    // =================================================================
    //  내부 헬퍼
    // =================================================================
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private Long currentMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Long id) ? id : null;
    }
}