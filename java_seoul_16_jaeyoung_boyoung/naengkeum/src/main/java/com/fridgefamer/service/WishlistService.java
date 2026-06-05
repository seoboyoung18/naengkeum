package com.fridgefamer.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.wishlist.AiRecipeDetail;
import com.fridgefamer.dto.response.wishlist.AiRecipeRow;
import com.fridgefamer.dto.response.wishlist.AiRecipeSaved;
import com.fridgefamer.dto.response.wishlist.WishlistItem;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.wishlist.WishlistMapper;
import com.fridgefamer.mapper.wishlist.WishlistMapper.AiRecipeCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 찜(Wishlist) 도메인 서비스 — API 명세 §6.
 *
 * <p>전부 인증 필수. 일반 레시피 찜은 토글(중복 409), AI 레시피는 ai_recipe 저장 후 연결한다.
 * AI 찜 해제는 ai_recipe 삭제(FK CASCADE로 wishlist 동반 삭제)이며 소유자만 가능(404/403).</p>
 */
@Service
public class WishlistService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final WishlistMapper wishlistMapper;

    public WishlistService(WishlistMapper wishlistMapper) {
        this.wishlistMapper = wishlistMapper;
    }

    // =====================================================================
    //  GET /api/wishlist/ai/{aiRecipeId}  — 저장한 AI 레시피 단건 상세
    // =====================================================================
    public AiRecipeDetail getAiRecipe(Long memberId, Long aiRecipeId) {
        AiRecipeRow row = wishlistMapper.selectAiRecipe(aiRecipeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "AI 레시피를 찾을 수 없습니다");
        }
        if (!row.memberId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 AI 레시피만 조회할 수 있습니다");
        }
        return new AiRecipeDetail(
                row.aiRecipeId(), row.title(), row.summary(),
                parseJson(row.ingredientsJson()), parseJson(row.stepsJson()),
                row.cookTime(), row.createdAt());
    }

    /** ai_recipe의 JSON 문자열 → JsonNode(파싱 실패 시 null). */
    private JsonNode parseJson(String s) {
        if (s == null) return null;
        try {
            return JSON.readTree(s);
        } catch (Exception e) {
            return null;
        }
    }

    // =====================================================================
    //  GET /api/wishlist/me
    // =====================================================================
    public PageResponse<WishlistItem> list(Long memberId, int page, int size) {
        int offset = page * size;
        List<WishlistItem> content = wishlistMapper.selectByMember(memberId, offset, size);
        long total = wishlistMapper.countByMember(memberId);
        return PageResponse.of(content, page, size, total);
    }

    // =====================================================================
    //  POST /api/wishlist/{recipeId}  — 일반 레시피 찜 등록
    // =====================================================================
    @Transactional
    public void addRecipe(Long memberId, Long recipeId) {
        wishlistMapper.insertRecipeWishlist(memberId, recipeId);  // 중복→409, 없는 레시피→400
    }

    // =====================================================================
    //  DELETE /api/wishlist/{recipeId}  — 일반 레시피 찜 해제(멱등)
    // =====================================================================
    @Transactional
    public void removeRecipe(Long memberId, Long recipeId) {
        wishlistMapper.deleteRecipeWishlist(memberId, recipeId);
    }

    // =====================================================================
    //  POST /api/wishlist/ai  — AI 레시피 찜 저장
    // =====================================================================
    @Transactional
    public AiRecipeSaved saveAi(Long memberId, SaveAiRecipeRequest req) {
        AiRecipeCommand cmd = new AiRecipeCommand(
                memberId,
                req.title(),
                req.summary(),
                toJsonArray(req.ingredientsJson()),
                toJsonArray(req.stepsJson()),
                req.cookTime());
        wishlistMapper.insertAiRecipe(cmd);
        wishlistMapper.insertAiWishlist(memberId, cmd.getAiRecipeId());
        return new AiRecipeSaved(cmd.getAiRecipeId());
    }

    // =====================================================================
    //  DELETE /api/wishlist/ai/{aiRecipeId}  — AI 레시피 찜 해제
    // =====================================================================
    @Transactional
    public void removeAi(Long memberId, Long aiRecipeId) {
        Long ownerId = wishlistMapper.findAiRecipeOwner(aiRecipeId);
        if (ownerId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "AI 레시피 찜 기록을 찾을 수 없습니다");
        }
        if (!ownerId.equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 AI 레시피 찜만 해제할 수 있습니다");
        }
        wishlistMapper.deleteAiRecipe(aiRecipeId);  // FK CASCADE로 wishlist 동반 삭제
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    /**
     * JsonNode 리스트 → JSON 배열 문자열. ObjectMapper 빈에 의존하지 않도록
     * 각 노드의 표준 JSON 표현(JsonNode#toString)을 합친다(프로젝트 방침: Jackson 빈 비의존).
     * 결과는 항상 배열이라 ai_recipe의 CHECK(JSON_TYPE=ARRAY)를 만족한다.
     */
    private String toJsonArray(List<JsonNode> nodes) {
        return nodes.stream()
                .map(JsonNode::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
