package com.fridgefamer.dto.response.seasoning;

/**
 * 조미료 카탈로그 단건 응답 — 마스터 목록 + 권장 보관 위치 + "내 보유 여부".
 *
 * <pre>
 * { "seasoningId": 1, "name": "소금", "storageType": "ROOM_TEMP",
 *   "storageTip": "습기 차단하여 밀폐 보관.", "owned": true }
 * </pre>
 *
 * <p>owned = member_seasoning에 해당 행이 존재하는지 여부(SQL에서 계산).
 * storageType(FRIDGE/FREEZER/ROOM_TEMP)으로 냉장·냉동·실온 분류가 가능하다.
 * 조미료는 수량/유통기한 없이 보유 여부만 관리한다.</p>
 */
public record SeasoningItem(
        Long seasoningId,
        String name,
        String storageType,
        String storageTip,
        boolean owned
) {}
