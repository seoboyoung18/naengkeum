package com.fridgefamer.dto.request.fridge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POST /api/fridge · PUT /api/fridge/{id} 요청 Body.
 *
 * <p>등록/수정 본문이 동일하므로 하나의 record를 공유한다(API 명세 §3 "수정 = 등록과 동일").
 * storageType은 ENUM('FRIDGE','FREEZER','ROOM_TEMP') — 문자열 검증으로 방어.
 * qty는 DECIMAL(10,2) 매핑 + chk_fridge_qty_positive(qty >= 0) 제약에 맞춰 0 이상.</p>
 */
public record FridgeItemRequest(
        @NotBlank(message = "재료명은 필수입니다")
        @Size(max = 50, message = "재료명은 50자 이하여야 합니다")
        String name,

        @NotNull(message = "수량은 필수입니다")
        @PositiveOrZero(message = "수량은 0 이상이어야 합니다")
        BigDecimal qty,

        @NotBlank(message = "단위는 필수입니다")
        @Size(max = 10, message = "단위는 10자 이하여야 합니다")
        String unit,

        @NotBlank(message = "보관 위치는 필수입니다")
        @Pattern(regexp = "FRIDGE|FREEZER|ROOM_TEMP",
                message = "보관 위치는 FRIDGE, FREEZER, ROOM_TEMP 중 하나여야 합니다")
        String storageType,

        @NotNull(message = "유통기한은 필수입니다")
        LocalDate expiryDate,

        @Size(max = 100, message = "메모는 100자 이하여야 합니다")
        String memo
) {}
