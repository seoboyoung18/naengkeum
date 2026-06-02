package com.fridgefamer.dto.response.fridge;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 냉장고 재료 단건 응답 — 목록/대시보드/등록·수정 응답에 공통 사용.
 *
 * <pre>
 * { "fridgeItemId":1, "name":"계란", "qty":6, "unit":"개",
 *   "storageType":"FRIDGE", "expiryDate":"2026-05-16", "dDay":-1, "memo":null }
 * </pre>
 *
 * <p>dDay = DATEDIFF(expiry_date, CURDATE()). 음수면 이미 지난 재료.
 * qty는 DECIMAL(10,2)이므로 BigDecimal로 매핑한다.</p>
 */
public record FridgeItem(
        Long fridgeItemId,
        String name,
        BigDecimal qty,
        String unit,
        String storageType,
        LocalDate expiryDate,
        int dDay,
        String memo
) {}
