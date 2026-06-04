package com.fridgefamer.controller;

import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.wishlist.AiRecipeSaved;
import com.fridgefamer.dto.response.wishlist.WishlistItem;
import com.fridgefamer.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 찜(Wishlist) API — API 명세 §6.
 *
 * <ul>
 *   <li>GET    /api/wishlist/me            — F15 찜 목록(일반+AI, 페이징)</li>
 *   <li>POST   /api/wishlist/{recipeId}    — F15 일반 레시피 찜 등록(중복 409)</li>
 *   <li>DELETE /api/wishlist/{recipeId}    — F15 일반 레시피 찜 해제</li>
 *   <li>POST   /api/wishlist/ai            — F19 AI 레시피 찜 저장(201)</li>
 *   <li>DELETE /api/wishlist/ai/{aiRecipeId} — F19 AI 레시피 찜 해제(403/404)</li>
 * </ul>
 *
 * <p>전부 인증 필요(SecurityConfig anyRequest().authenticated()).
 * /ai 정적 경로가 /{recipeId}보다 우선 매칭된다.</p>
 */
@RestController
@RequestMapping("/api/wishlist")
@Validated
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/me")
    public PageResponse<WishlistItem> myWishlist(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size는 1 이상이어야 합니다") int size
    ) {
        return wishlistService.list(currentMemberId(), page, size);
    }

    @PostMapping("/{recipeId}")
    public Map<String, Boolean> add(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        wishlistService.addRecipe(currentMemberId(), recipeId);
        return Map.of("wishlisted", true);
    }

    @DeleteMapping("/{recipeId}")
    public Map<String, Boolean> remove(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        wishlistService.removeRecipe(currentMemberId(), recipeId);
        return Map.of("wishlisted", false);
    }

    @PostMapping("/ai")
    public ResponseEntity<AiRecipeSaved> saveAi(@Valid @RequestBody SaveAiRecipeRequest req) {
        AiRecipeSaved saved = wishlistService.saveAi(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/ai/{aiRecipeId}")
    public Map<String, Boolean> removeAi(
            @PathVariable @Positive(message = "aiRecipeId는 양수여야 합니다") Long aiRecipeId
    ) {
        wishlistService.removeAi(currentMemberId(), aiRecipeId);
        return Map.of("wishlisted", false);
    }

    // =================================================================
    //  내부 헬퍼 — 인증 필수
    // =================================================================
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
