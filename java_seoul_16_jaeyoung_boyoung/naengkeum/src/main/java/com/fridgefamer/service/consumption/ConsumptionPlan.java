package com.fridgefamer.service.consumption;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레시피 공개 시 냉장고 재고 차감 "계획" — 실제 DB 변경 전 계산 결과.
 *
 * <p>{@link #deductions()}는 차감될 항목(냉장고 row와 차감량/차감 후 수량),
 * {@link #skipped()}는 차감되지 않은 재료와 사유다. 응답 토스트("앞다리살 200g 차감됨"),
 * 실제 차감 실행(차감/삭제 쿼리) 모두 이 계획을 그대로 따른다.</p>
 */
public record ConsumptionPlan(
        List<Deduction> deductions,
        List<SkippedIngredient> skipped
) {

    /** 차감 1건 — 냉장고 항목 기준. removeItem=true면 차감 후 0이라 항목 삭제 대상. */
    public record Deduction(
            Long fridgeItemId,
            String name,
            String unit,
            BigDecimal before,   // 차감 전 수량
            BigDecimal used,     // 차감량(냉장고 단위로 환산된 값)
            BigDecimal after,    // 차감 후 수량(0 클램프)
            boolean removeItem   // after==0 → 냉장고에서 삭제
    ) {}

    /** 차감되지 않은 재료 + 사유. 토스트/로그에서 "왜 안 빠졌는지" 설명용. */
    public record SkippedIngredient(
            String name,
            SkipReason reason
    ) {}

    /** 차감 제외 사유. label은 사용자 노출용 한글. */
    public enum SkipReason {
        SEASONING("조미료"),
        NOT_QUANTITATIVE("수량 표기 아님"),
        NO_MATCH("냉장고에 없음"),
        UNIT_MISMATCH("단위 불일치");

        private final String label;
        SkipReason(String label) { this.label = label; }
        public String label() { return label; }
    }
}
