package com.fridgefamer.controller;

import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.wishlist.AiRecipeSavedResponse;
import com.fridgefamer.dto.response.wishlist.WishlistItem;
import com.fridgefamer.dto.response.wishlist.WishlistToggleResponse;
import com.fridgefamer.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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

/**
 * 찜(Wishlist) API — API 명세 §6. 전부 인증 필요.
 *
 * <ul>
 *   <li>GET    /api/wishlist/me               — F15 찜 목록</li>
 *   <li>POST   /api/wishlist/{recipeId}       — F15 찜 등록 (이미 찜 409)</li>
 *   <li>DELETE /api/wishlist/{recipeId}       — F15 찜 해제</li>
 *   <li>POST   /api/wishlist/ai               — F19 AI 레시피 저장 (201)</li>
 *   <li>DELETE /api/wishlist/ai/{aiRecipeId}  — F19 AI 찜 해제 (403/404)</li>
 * </ul>
 *
 * <p>경로 충돌 주의: /ai 고정 경로가 /{recipeId} 변수 경로보다 먼저 매칭되도록
 * Spring이 처리하지만, AI 관련은 /ai 프리픽스로 명확히 분리했다.</p>
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
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이어야 합니다")
            int page,

            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "size는 1 이상이어야 합니다")
            @Max(value = 100, message = "size는 100 이하여야 합니다")
            int size
    ) {
        return wishlistService.myWishlist(currentMemberId(), page, size);
    }

    @PostMapping("/ai")
    public ResponseEntity<AiRecipeSavedResponse> saveAiRecipe(
            @Valid @RequestBody SaveAiRecipeRequest req
    ) {
        AiRecipeSavedResponse saved = wishlistService.saveAiRecipe(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/ai/{aiRecipeId}")
    public WishlistToggleResponse removeAiRecipe(
            @PathVariable @Positive(message = "aiRecipeId는 양수여야 합니다") Long aiRecipeId
    ) {
        return wishlistService.removeAiRecipe(currentMemberId(), aiRecipeId);
    }

    @PostMapping("/{recipeId}")
    public WishlistToggleResponse addRecipeWish(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        return wishlistService.addRecipeWish(currentMemberId(), recipeId);
    }

    @DeleteMapping("/{recipeId}")
    public WishlistToggleResponse removeRecipeWish(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        return wishlistService.removeRecipeWish(currentMemberId(), recipeId);
    }

    // =================================================================
    //  내부 헬퍼 — 전부 인증 필요라 principal은 항상 Long
    // =================================================================
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}