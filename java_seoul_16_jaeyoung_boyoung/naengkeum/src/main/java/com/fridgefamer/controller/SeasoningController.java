package com.fridgefamer.controller;

import com.fridgefamer.dto.request.seasoning.SeasoningOwnRequest;
import com.fridgefamer.dto.response.seasoning.SeasoningItem;
import com.fridgefamer.service.SeasoningService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 조미료(Seasoning) API.
 *
 * <ul>
 *   <li>GET /api/seasonings — 조미료 카탈로그 + 내 보유여부</li>
 *   <li>PUT /api/seasonings — 보유 조미료 집합 저장(선택 완료) → 갱신 목록 반환</li>
 * </ul>
 *
 * <p>모든 엔드포인트 인증 필요(SecurityConfig anyRequest().authenticated()).
 * memberId는 FridgeController와 동일하게 SecurityContext에서 추출한다.</p>
 */
@RestController
@RequestMapping("/api/seasonings")
public class SeasoningController {

    private final SeasoningService seasoningService;

    public SeasoningController(SeasoningService seasoningService) {
        this.seasoningService = seasoningService;
    }

    @GetMapping
    public List<SeasoningItem> list() {
        return seasoningService.list(currentMemberId());
    }

    @PutMapping
    public List<SeasoningItem> save(@RequestBody SeasoningOwnRequest req) {
        return seasoningService.saveOwned(currentMemberId(), req.seasoningIds());
    }

    // =================================================================
    //  내부 헬퍼 — SecurityContext에서 memberId 추출 (FridgeController와 동일)
    // =================================================================
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
