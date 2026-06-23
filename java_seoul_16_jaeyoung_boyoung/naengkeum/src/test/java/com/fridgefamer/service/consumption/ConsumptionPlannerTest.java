package com.fridgefamer.service.consumption;

import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.service.consumption.ConsumptionPlan.Deduction;
import com.fridgefamer.service.consumption.ConsumptionPlan.SkipReason;
import com.fridgefamer.service.consumption.ConsumptionPlan.SkippedIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumptionPlannerTest {

    // V15 조미료 카탈로그 중 이 테스트에 등장하는 이름들.
    private static final Set<String> SEASONINGS = Set.of(
            "소금", "설탕", "후추", "식초", "간장", "고춧가루", "참기름");

    private static RecipeIngredient ing(String name, String qty) {
        return new RecipeIngredient(name, qty);
    }

    private static FridgeItem fridge(long id, String name, String qty, String unit) {
        return new FridgeItem(id, name, new BigDecimal(qty), unit,
                "FRIDGE", null, 0, null);
    }

    private static Map<String, Deduction> byName(ConsumptionPlan plan) {
        return plan.deductions().stream()
                .collect(Collectors.toMap(Deduction::name, Function.identity()));
    }

    private static Map<String, SkipReason> skipByName(ConsumptionPlan plan) {
        return plan.skipped().stream()
                .collect(Collectors.toMap(SkippedIngredient::name, SkippedIngredient::reason));
    }

    @Test
    @DisplayName("실데이터 시나리오 — 돼지불고기 레시피 × 실제 냉장고")
    void realScenario() {
        // 스크린샷의 레시피 재료 12종
        List<RecipeIngredient> recipe = List.of(
                ing("돼지고기", "200g"),
                ing("상추", "4개"),
                ing("대파", "1개"),
                ing("고추", "2개"),
                ing("양파", "0.5개"),
                ing("고춧가루", "2큰술"),   // 조미료
                ing("간장", "1큰술"),       // 조미료
                ing("식초", "1작은술"),     // 조미료
                ing("쌀밥", "2공기"),
                ing("참기름", "1작은술"),   // 조미료
                ing("설탕", "0.5작은술"),   // 조미료
                ing("마늘", "1쪽")
        );
        // 스크린샷의 냉장고 10종 (돼지고기 없음·삼겹살만, 상추·마늘 없음, 쌀은 있지만 "쌀밥"은 없음)
        List<FridgeItem> fridgeItems = List.of(
                fridge(1, "광어", "1000", "g"),
                fridge(2, "삼겹살", "400", "g"),
                fridge(3, "오이", "1", "개"),
                fridge(4, "가지", "1", "개"),
                fridge(5, "대파", "1", "개"),
                fridge(6, "고추", "5", "개"),
                fridge(7, "청양고추", "5", "개"),
                fridge(8, "양파", "1", "개"),
                fridge(9, "쌀", "5000", "g"),
                fridge(10, "닭가슴살", "1000", "g")
        );

        ConsumptionPlan plan = ConsumptionPlanner.plan(recipe, fridgeItems, SEASONINGS);

        // --- 차감되는 것: 대파·고추·양파 3종 ---
        Map<String, Deduction> ded = byName(plan);
        assertEquals(3, ded.size(), "차감은 정확히 3종(대파·고추·양파)");

        Deduction daepa = ded.get("대파");
        assertEquals(0, new BigDecimal("0").compareTo(daepa.after()));
        assertTrue(daepa.removeItem(), "대파 1개 → 0 → 삭제 대상");

        Deduction gochu = ded.get("고추");
        assertEquals(0, new BigDecimal("3").compareTo(gochu.after()), "고추 5→3");
        assertTrue(!gochu.removeItem());

        Deduction yangpa = ded.get("양파");
        assertEquals(0, new BigDecimal("0.5").compareTo(yangpa.after()), "양파 1→0.5");
        assertTrue(!yangpa.removeItem());

        // --- 스킵되는 것: 조미료 5 + NO_MATCH 4(돼지고기·상추·쌀밥·마늘) ---
        Map<String, SkipReason> skip = skipByName(plan);
        assertEquals(SkipReason.NO_MATCH, skip.get("돼지고기"));
        assertEquals(SkipReason.NO_MATCH, skip.get("상추"));
        assertEquals(SkipReason.NO_MATCH, skip.get("쌀밥"));
        assertEquals(SkipReason.NO_MATCH, skip.get("마늘"));
        assertEquals(SkipReason.SEASONING, skip.get("고춧가루"));
        assertEquals(SkipReason.SEASONING, skip.get("설탕"));
        assertEquals(9, plan.skipped().size(), "스킵 9종(조미료5 + 미보유4)");
    }

    @Test
    @DisplayName("kg↔g 변환 차감 — 냉장고 1000g, 레시피 0.5kg → 500g 남음")
    void weightConversion() {
        ConsumptionPlan plan = ConsumptionPlanner.plan(
                List.of(ing("닭가슴살", "0.5kg")),
                List.of(fridge(1, "닭가슴살", "1000", "g")),
                Set.of());
        Deduction d = plan.deductions().get(0);
        assertEquals(0, new BigDecimal("500").compareTo(d.after()));
        assertTrue(!d.removeItem());
    }

    @Test
    @DisplayName("단위 불일치는 스킵 — 냉장고 계란 6개, 레시피 계란 100g")
    void unitMismatchSkipped() {
        ConsumptionPlan plan = ConsumptionPlanner.plan(
                List.of(ing("계란", "100g")),
                List.of(fridge(1, "계란", "6", "개")),
                Set.of());
        assertTrue(plan.deductions().isEmpty());
        assertEquals(SkipReason.UNIT_MISMATCH, plan.skipped().get(0).reason());
    }

    @Test
    @DisplayName("조미료는 냉장고에 있어도 차감 안 함 — 유무만 판단")
    void seasoningNeverDeducted() {
        ConsumptionPlan plan = ConsumptionPlanner.plan(
                List.of(ing("설탕", "100g")),
                List.of(fridge(1, "설탕", "500", "g")),  // 설탕이 냉장고에 있어도
                SEASONINGS);
        assertTrue(plan.deductions().isEmpty(), "조미료는 차감 제외");
        assertEquals(SkipReason.SEASONING, plan.skipped().get(0).reason());
    }

    @Test
    @DisplayName("재고 초과 사용은 0으로 클램프 + 삭제 (방어)")
    void overUseClampsToZeroAndRemoves() {
        ConsumptionPlan plan = ConsumptionPlanner.plan(
                List.of(ing("대파", "5개")),
                List.of(fridge(1, "대파", "1", "개")),
                Set.of());
        Deduction d = plan.deductions().get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(d.after()));
        assertTrue(d.removeItem());
    }

    @Test
    @DisplayName("같은 재료가 레시피에 두 번 나오면 누적 차감")
    void duplicateIngredientAccumulates() {
        ConsumptionPlan plan = ConsumptionPlanner.plan(
                List.of(ing("양파", "0.5개"), ing("양파", "0.25개")),
                List.of(fridge(1, "양파", "1", "개")),
                Set.of());
        assertEquals(2, plan.deductions().size());
        // 두 번째 차감의 after = 1 - 0.5 - 0.25 = 0.25
        Deduction second = plan.deductions().get(1);
        assertEquals(0, new BigDecimal("0.25").compareTo(second.after()));
    }
}
