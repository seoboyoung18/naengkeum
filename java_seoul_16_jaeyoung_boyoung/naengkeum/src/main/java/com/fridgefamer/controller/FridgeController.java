package com.fridgefamer.controller;

import com.fridgefamer.dto.request.fridge.FridgeItemRequest;
import com.fridgefamer.dto.response.fridge.FridgeDashboardResponse;
import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.fridge.FridgeListResponse;
import com.fridgefamer.dto.response.fridge.FridgeMatchResponse;
import com.fridgefamer.service.FridgeService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 냉장고(Fridge) API — API 명세 §3.
 *
 * <ul>
 *   <li>GET    /api/fridge             — F02 재료 목록 (storageType·sort 필터)</li>
 *   <li>POST   /api/fridge             — F01 재료 등록 (201)</li>
 *   <li>PUT    /api/fridge/{id}        — F03 재료 수정</li>
 *   <li>DELETE /api/fridge/{id}        — F04 재료 삭제</li>
 *   <li>GET    /api/fridge/dashboard   — F17 요약 + D-3 임박 목록</li>
 *   <li>GET    /api/fridge/match       — F02 레시피 보유재료 매칭</li>
 * </ul>
 *
 * <p>모든 엔드포인트 인증 필요(SecurityConfig anyRequest().authenticated()).</p>
 */
@RestController
@RequestMapping("/api/fridge")
@Validated   // @RequestParam(@Pattern 등) 검증 트리거
public class FridgeController {

    private final FridgeService fridgeService;

    public FridgeController(FridgeService fridgeService) {
        this.fridgeService = fridgeService;
    }

    @GetMapping
    public FridgeListResponse list(
            @RequestParam(defaultValue = "ALL")
            @Pattern(regexp = "FRIDGE|FREEZER|ROOM_TEMP|ALL",
                    message = "storageType은 FRIDGE, FREEZER, ROOM_TEMP, ALL 중 하나여야 합니다")
            String storageType,

            @RequestParam(defaultValue = "EXPIRY_ASC")
            @Pattern(regexp = "EXPIRY_ASC|CREATED_DESC|NAME_ASC",
                    message = "sort는 EXPIRY_ASC, CREATED_DESC, NAME_ASC 중 하나여야 합니다")
            String sort
    ) {
        return fridgeService.list(currentMemberId(), storageType, sort);
    }

    @PostMapping
    public ResponseEntity<FridgeItem> create(@Valid @RequestBody FridgeItemRequest req) {
        FridgeItem created = fridgeService.create(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{fridgeItemId}")
    public FridgeItem update(
            @PathVariable @Positive(message = "fridgeItemId는 양수여야 합니다") Long fridgeItemId,
            @Valid @RequestBody FridgeItemRequest req
    ) {
        return fridgeService.update(currentMemberId(), fridgeItemId, req);
    }

    @DeleteMapping("/{fridgeItemId}")
    public Map<String, String> delete(
            @PathVariable @Positive(message = "fridgeItemId는 양수여야 합니다") Long fridgeItemId
    ) {
        fridgeService.delete(currentMemberId(), fridgeItemId);
        return Map.of("message", "삭제 완료");
    }

    @GetMapping("/dashboard")
    public FridgeDashboardResponse dashboard() {
        return fridgeService.dashboard(currentMemberId());
    }

    @GetMapping("/match")
    public FridgeMatchResponse match(
            @RequestParam @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        return fridgeService.match(currentMemberId(), recipeId);
    }

    // =================================================================
    //  내부 헬퍼 — SecurityContext에서 memberId 추출
    // =================================================================

    /** 인증 필수. Security 필터를 통과했다면 principal은 Long. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
