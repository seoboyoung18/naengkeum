package com.fridgefamer.dto.response.ingredient;

/**
 * 식재료 보관기한/보관법 제안 응답 — API 명세 §12 F22.
 *
 * <pre>
 * { "name":"양파", "category":"채소", "defaultStorageType":"ROOM_TEMP",
 *   "fridgeDays":60, "freezerDays":180, "roomTempDays":14,
 *   "storageTip":"...", "found":true }
 * </pre>
 *
 * <p>사전에 없는 식재료는 404 대신 <b>직접입력 모드 폴백</b>으로 응답한다
 * ({@code found=false}, 보관일수 null). 종 수보다 폴백 커버리지가 핵심이므로
 * 프론트는 {@code found} 플래그로 "사전값 자동제안" vs "직접입력" 분기한다.</p>
 *
 * <p>보관 일수 컬럼(fridge/freezer/room_temp_days)은 DB에서 NULL 가능하므로
 * 박싱 타입 Integer로 매핑한다.</p>
 */
public record IngredientSuggestResponse(
        String name,
        String category,
        String defaultStorageType,
        Integer fridgeDays,
        Integer freezerDays,
        Integer roomTempDays,
        String storageTip,
        boolean found
) {}
