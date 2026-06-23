package com.fridgefamer.service.consumption;

import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.service.consumption.ConsumptionPlan.Deduction;
import com.fridgefamer.service.consumption.ConsumptionPlan.SkipReason;
import com.fridgefamer.service.consumption.ConsumptionPlan.SkippedIngredient;
import com.fridgefamer.util.QtyParser;
import com.fridgefamer.util.QtyParser.ParsedQty;
import com.fridgefamer.util.UnitConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 레시피 재료 × 냉장고 재고 × 조미료 카탈로그 → 차감 계획 산출(순수 함수).
 *
 * <p>판정 순서(재료 1건당):
 * <ol>
 *   <li>이름이 조미료 카탈로그에 있으면 → 스킵(SEASONING). 조미료는 양 무관 미차감.</li>
 *   <li>수량이 정량 파싱 안 되면("적당량" 등) → 스킵(NOT_QUANTITATIVE).</li>
 *   <li>냉장고에 같은 이름이 없으면 → 스킵(NO_MATCH). 매칭은 이름 정확 일치(SQL과 동일).</li>
 *   <li>레시피 단위를 냉장고 단위로 환산 불가면 → 스킵(UNIT_MISMATCH).</li>
 *   <li>그 외 → 차감(0 클램프, 0이면 항목 삭제).</li>
 * </ol>
 *
 * <p>이름이 같은 냉장고 항목이 여럿이면 입력 리스트의 첫 항목에 차감한다 —
 * 호출 측이 임박순(EXPIRY_ASC)으로 넘기면 유통기한이 가까운 것부터 소비된다.
 * 같은 레시피에 같은 재료가 중복 등장해도 각각 같은 항목에서 누적 차감된다.</p>
 *
 * <p>DB를 건드리지 않는다. 실제 차감/삭제는 이 계획을 받아 상위 서비스가 수행한다.</p>
 */
public final class ConsumptionPlanner {

    private ConsumptionPlanner() {}

    /**
     * @param ingredients     레시피 재료(name, qty 자유텍스트)
     * @param fridgeItems     회원 냉장고 항목(이름 정확 매칭, qty/unit 보유). 임박순 권장.
     * @param seasoningNames  조미료 카탈로그 이름 집합(이 이름의 재료는 무조건 스킵)
     */
    public static ConsumptionPlan plan(List<RecipeIngredient> ingredients,
                                       List<FridgeItem> fridgeItems,
                                       Set<String> seasoningNames) {
        // 이름 → 가용 항목(차감하며 잔량 갱신). 첫 등장 항목 우선(임박순 입력 시 임박분 우선).
        Map<String, MutableItem> byName = new LinkedHashMap<>();
        for (FridgeItem fi : fridgeItems) {
            byName.putIfAbsent(key(fi.name()), new MutableItem(fi));
        }

        List<Deduction> deductions = new ArrayList<>();
        List<SkippedIngredient> skipped = new ArrayList<>();

        for (RecipeIngredient ing : ingredients) {
            String name = ing.name() == null ? "" : ing.name().trim();

            if (seasoningNames.contains(key(name))) {
                skipped.add(new SkippedIngredient(name, SkipReason.SEASONING));
                continue;
            }
            ParsedQty parsed = QtyParser.parse(ing.qty());
            if (parsed == null) {
                skipped.add(new SkippedIngredient(name, SkipReason.NOT_QUANTITATIVE));
                continue;
            }
            MutableItem item = byName.get(key(name));
            if (item == null) {
                skipped.add(new SkippedIngredient(name, SkipReason.NO_MATCH));
                continue;
            }
            BigDecimal used = UnitConverter.convert(parsed.amount(), parsed.unit(), item.unit);
            if (used == null) {
                skipped.add(new SkippedIngredient(name, SkipReason.UNIT_MISMATCH));
                continue;
            }

            BigDecimal before = item.remaining;
            BigDecimal after = before.subtract(used);
            if (after.signum() < 0) after = BigDecimal.ZERO;   // 재고 초과 방어(설계상 없음)
            item.remaining = after;                            // 같은 재료 중복 등장 누적 반영

            boolean remove = after.signum() == 0;
            deductions.add(new Deduction(
                    item.fridgeItemId, item.name, item.unit, before, used, after, remove));
        }
        return new ConsumptionPlan(deductions, skipped);
    }

    /** 이름 매칭 키 — 공백 차이만 흡수(트림). 그 외는 정확 일치 유지(SQL fi.name=ri.name 방침). */
    private static String key(String name) {
        return name == null ? "" : name.trim();
    }

    /** 차감하며 잔량을 갱신하기 위한 가변 래퍼. */
    private static final class MutableItem {
        final Long fridgeItemId;
        final String name;
        final String unit;
        BigDecimal remaining;

        MutableItem(FridgeItem fi) {
            this.fridgeItemId = fi.fridgeItemId();
            this.name = fi.name();
            this.unit = fi.unit();
            this.remaining = fi.qty();
        }
    }
}
