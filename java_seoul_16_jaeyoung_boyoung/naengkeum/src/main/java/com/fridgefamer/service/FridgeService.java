package com.fridgefamer.service;

import com.fridgefamer.dto.request.fridge.FridgeItemRequest;
import com.fridgefamer.dto.response.fridge.FridgeDashboardResponse;
import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.fridge.FridgeListResponse;
import com.fridgefamer.dto.response.fridge.FridgeMatchResponse;
import com.fridgefamer.dto.response.fridge.FridgeSummary;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.fridge.FridgeMapper;
import com.fridgefamer.mapper.fridge.FridgeMapper.FridgeItemCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 냉장고(Fridge) 도메인 서비스 — API 명세 §3.
 *
 * <p>모든 흐름은 Controller가 SecurityContext에서 추출한 memberId를 받는다.
 * 수정/삭제는 소유자 검증(타인 리소스 → 403, 없음 → 404) 후 진행한다.</p>
 */
@Service
public class FridgeService {

    /** 대시보드 임박 기준 — D-3 이내(API 명세 §3 F17). */
    private static final int IMMINENT_DAYS = 3;

    private final FridgeMapper fridgeMapper;

    public FridgeService(FridgeMapper fridgeMapper) {
        this.fridgeMapper = fridgeMapper;
    }

    // =====================================================================
    //  GET /api/fridge
    // =====================================================================
    public FridgeListResponse list(Long memberId, String storageType, String sort) {
        List<FridgeItem> items = fridgeMapper.selectByMember(memberId, storageType, sort);
        FridgeSummary summary = fridgeMapper.selectSummary(memberId);
        return new FridgeListResponse(items, summary);
    }

    // =====================================================================
    //  POST /api/fridge
    // =====================================================================
    @Transactional
    public FridgeItem create(Long memberId, FridgeItemRequest req) {
        FridgeItemCommand cmd = toCommand(memberId, req);
        fridgeMapper.insertFridgeItem(cmd);
        return fridgeMapper.selectItemById(cmd.getFridgeItemId());
    }

    // =====================================================================
    //  PUT /api/fridge/{fridgeItemId}
    // =====================================================================
    @Transactional
    public FridgeItem update(Long memberId, Long fridgeItemId, FridgeItemRequest req) {
        verifyOwner(memberId, fridgeItemId);
        fridgeMapper.updateFridgeItem(fridgeItemId, toCommand(memberId, req));
        return fridgeMapper.selectItemById(fridgeItemId);
    }

    // =====================================================================
    //  DELETE /api/fridge/{fridgeItemId}
    // =====================================================================
    @Transactional
    public void delete(Long memberId, Long fridgeItemId) {
        verifyOwner(memberId, fridgeItemId);
        fridgeMapper.deleteById(fridgeItemId);
    }

    // =====================================================================
    //  GET /api/fridge/dashboard
    // =====================================================================
    public FridgeDashboardResponse dashboard(Long memberId) {
        FridgeSummary summary = fridgeMapper.selectSummary(memberId);
        List<FridgeItem> expiring = fridgeMapper.selectExpiring(memberId, IMMINENT_DAYS);
        return new FridgeDashboardResponse(summary, expiring);
    }

    // =====================================================================
    //  GET /api/fridge/match
    // =====================================================================
    public FridgeMatchResponse match(Long memberId, Long recipeId) {
        List<Long> owned = fridgeMapper.selectOwnedIngredientIds(memberId, recipeId);
        return new FridgeMatchResponse(owned);
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================

    /** 재료가 존재하고 호출자 소유인지 검증. 없으면 404, 타인 것이면 403. */
    private void verifyOwner(Long memberId, Long fridgeItemId) {
        Long ownerId = fridgeMapper.findOwnerId(fridgeItemId);
        if (ownerId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "재료를 찾을 수 없습니다");
        }
        if (!ownerId.equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 재료만 수정/삭제할 수 있습니다");
        }
    }

    private FridgeItemCommand toCommand(Long memberId, FridgeItemRequest req) {
        return new FridgeItemCommand(
                memberId,
                req.name(),
                req.qty(),
                req.unit(),
                req.storageType(),
                req.expiryDate(),
                req.memo()
        );
    }
}
