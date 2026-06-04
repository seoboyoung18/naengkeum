package com.fridgefamer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.wishlist.AiRecipeSavedResponse;
import com.fridgefamer.dto.response.wishlist.WishlistItem;
import com.fridgefamer.dto.response.wishlist.WishlistRecipeRow;
import com.fridgefamer.dto.response.wishlist.WishlistToggleResponse;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper.RecipeMainIngredientRow;
import com.fridgefamer.mapper.wishlist.WishlistMapper;
import com.fridgefamer.mapper.wishlist.WishlistMapper.AiRecipeCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 찜(Wishlist) 도메인 서비스 — API 명세 §6. 전부 인증 필요.
 *
 * <p>일반 레시피 찜과 AI 레시피 찜을 다룬다. wishlist 테이블은 XOR 제약
 * (recipe_id/ai_recipe_id 중 하나)이 있어, 각 INSERT는 한쪽 컬럼만 채운다.</p>
 *
 * <p>AI 저장은 ai_recipe INSERT(JSON 직렬화) → wishlist INSERT 2단계라 @Transactional로
 * 묶는다. 둘 중 하나라도 실패하면 롤백.</p>
 */
@Service
public class WishlistService {

    private final WishlistMapper wishlistMapper;
    private final ObjectMapper objectMapper;

    public WishlistService(WishlistMapper wishlistMapper, ObjectMapper objectMapper) {
        this.wishlistMapper = wishlistMapper;
        this.objectMapper = objectMapper;
    }

    // =====================================================================
    //  GET /api/wishlist/me — 찜 목록 (일반 레시피)
    // =====================================================================
    public PageResponse<WishlistItem> myWishlist(Long memberId, int page, int size) {
        int offset = page * size;
        List<WishlistRecipeRow> rows = wishlistMapper.selectRecipeWishes(memberId, offset, size);
        long total = wishlistMapper.countRecipeWishes(memberId);

        List<WishlistItem> items = toItems(rows);
        return PageResponse.of(items, page, size, total);
    }

    // =====================================================================
    //  POST /api/wishlist/{recipeId} — 일반 레시피 찜 등록
    // =====================================================================
    @Transactional
    public WishlistToggleResponse addRecipeWish(Long memberId, Long recipeId) {
        if (!wishlistMapper.recipeExists(recipeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        if (wishlistMapper.existsRecipeWish(memberId, recipeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 찜한 레시피입니다");
        }
        wishlistMapper.insertRecipeWish(memberId, recipeId);
        return new WishlistToggleResponse(true);
    }

    // =====================================================================
    //  DELETE /api/wishlist/{recipeId} — 일반 레시피 찜 해제
    // =====================================================================
    @Transactional
    public WishlistToggleResponse removeRecipeWish(Long memberId, Long recipeId) {
        wishlistMapper.deleteRecipeWish(memberId, recipeId);
        // 멱등 처리: 찜이 없었어도 결과는 "찜 안 된 상태"로 동일하게 false 반환
        return new WishlistToggleResponse(false);
    }

    // =====================================================================
    //  POST /api/wishlist/ai — AI 레시피 저장
    // =====================================================================
    @Transactional
    public AiRecipeSavedResponse saveAiRecipe(Long memberId, SaveAiRecipeRequest req) {
        String ingredientsJson = toJsonArray(req.ingredientsJson(), "재료");
        String stepsJson = toJsonArray(req.stepsJson(), "조리 순서");

        AiRecipeCommand cmd = new AiRecipeCommand(
                memberId, req.title(), req.summary(),
                ingredientsJson, stepsJson, req.cookTime());

        wishlistMapper.insertAiRecipe(cmd);                       // ai_recipe INSERT → id 회수
        wishlistMapper.insertAiWish(memberId, cmd.getAiRecipeId()); // wishlist INSERT (ai_recipe_id)

        return new AiRecipeSavedResponse(cmd.getAiRecipeId());
    }

    // =====================================================================
    //  DELETE /api/wishlist/ai/{aiRecipeId} — AI 레시피 찜 해제
    // =====================================================================
    @Transactional
    public WishlistToggleResponse removeAiRecipe(Long memberId, Long aiRecipeId) {
        Long ownerId = wishlistMapper.findAiRecipeOwner(aiRecipeId);
        if (ownerId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "AI 레시피를 찾을 수 없습니다");
        }
        if (!ownerId.equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 AI 레시피만 삭제할 수 있습니다");
        }
        // ai_recipe 삭제 → wishlist는 CASCADE로 함께 제거
        wishlistMapper.deleteAiRecipe(aiRecipeId);
        return new WishlistToggleResponse(false);
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    /** List&lt;String&gt; → JSON 배열 문자열. 직렬화 실패 시 400. */
    private String toJsonArray(List<String> list, String fieldLabel) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, fieldLabel + " 형식이 올바르지 않습니다");
        }
    }

    /** 찜 행 → 항목 조립(대표재료 묶음 주입). */
    private List<WishlistItem> toItems(List<WishlistRecipeRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> recipeIds = rows.stream().map(WishlistRecipeRow::recipeId).toList();

        Map<Long, List<String>> mainMap = new LinkedHashMap<>();
        for (RecipeMainIngredientRow mi : wishlistMapper.selectMainIngredients(recipeIds)) {
            mainMap.computeIfAbsent(mi.recipeId(), k -> new ArrayList<>()).add(mi.name());
        }

        List<WishlistItem> items = new ArrayList<>(rows.size());
        for (WishlistRecipeRow row : rows) {
            items.add(new WishlistItem(
                    row.wishlistId(),
                    "RECIPE",
                    row.recipeId(),
                    null,                    // aiRecipeId — 일반 찜이라 null
                    row.title(),
                    row.thumbnailUrl(),
                    row.cookTime(),
                    row.avgRating(),
                    row.reviewCount(),
                    mainMap.getOrDefault(row.recipeId(), List.of()),
                    row.wishlistedAt()
            ));
        }
        return items;
    }
}